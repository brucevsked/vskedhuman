package com.vsked.bci;

import org.apache.commons.math3.linear.RealMatrix;
import java.util.*;

/**
 * BCI 管线 v8 — 挂谷猜想驱动的 FB-RG (Kakeya-Filter-Bank Riemannian)
 *
 * 核心创新:
 *   三维挂谷猜想证明: R^3 中覆盖所有方向的"针集"必须达到维数 3。
 *   对 BCI: 切空间的完整 253 维中, 判别信息仅集中在 K << n 个主特征方向上。
 *   挂谷压缩将 253 维切空间压缩为 K + n + 1 维, 保留判别能力的同时杜绝过拟合。
 *
 * 管线:
 *   CAR
 *   → [5 频段] 带通滤波
 *     → 协方差矩阵 SPD(22,22)
 *     → 黎曼均值 G_band
 *     → 挂谷切空间特征 (K + n + 1 = 29 维)
 *   → 拼接 5 × 29 = 145 维
 *   → Z-score
 *   → ANOVA F-score 特征选择 → 60 维
 *   → Softmax 多分类
 *
 * 相比完整切空间 (5×253=1265维): 维度↓88%, 样本/特征比从 0.04→0.32
 */
public class BCIPipeline {

    public static final int[] ALL_CLASSES = {769, 770, 771, 772};
    public static final String[] CLASS_NAMES = {"左手", "右手", "双脚", "舌头"};

    // 频段: 覆盖 mu(8-12) / beta(12-24) / gamma(24-40)
    private static final double[][] FREQ_BANDS = {
        {4, 8}, {8, 12}, {12, 16}, {16, 24}, {24, 40},
    };

    // 挂谷方向数: 运动想象信号的主特征值通常 ≤ 6 (对应于 C3/C4/Cz 及对侧模式)
    private static final int KAKEYA_K = 6;

    // 特征选择保留数 (挂在挂谷特征拼接后)
    private static final int FS_KEEP = 60;

    private int numBands;
    private int numChannels;
    private int kakeyaDim;      // 每频段挂谷特征维度 = K + n + 1

    // 每频段的黎曼参考点
    private RealMatrix[] bandRefs;
    // 每频段的切空间 Z-score
    private double[][] bandMean;
    private double[][] bandStd;
    // 拼接后的 Z-score
    private double[] globalMean;
    private double[] globalStd;
    // 特征选择
    private int[] selectedIndices;
    // 分类器
    private SoftmaxRegression softmax;
    private boolean trained;

    // ==================== 训练 ====================

    public void train(List<Trial> trials) {
        System.out.println("=================================================================");
        System.out.println("BCI 管线 v8 — 挂谷猜想-FB-RG (Kakeya-Filter-Bank Riemannian)");
        System.out.println("=================================================================");

        numBands = FREQ_BANDS.length;
        numChannels = trials.get(0).data.length;
        kakeyaDim = KAKEYA_K + numChannels + 1;

        System.out.printf("频段: %d | 通道: %d | 挂谷方向 K=%d | 每频段特征: %d | 拼接后: %d%n",
                numBands, numChannels, KAKEYA_K, kakeyaDim, numBands * kakeyaDim);

        System.out.print("样本分布: ");
        Map<Integer, List<Trial>> classMap = new LinkedHashMap<>();
        for (int cls : ALL_CLASSES) classMap.put(cls, new ArrayList<>());
        for (Trial t : trials) classMap.get(t.label).add(t);
        for (int cls : ALL_CLASSES)
            System.out.printf("%s=%d  ", className(cls), classMap.get(cls).size());
        System.out.println();

        // [1] CAR
        System.out.println("\n[1/6] CAR 去噪...");
        for (Trial t : trials) SignalProcessor.applyCAR(t);

        // [2] 每频段: 滤波 → 协方差 → 黎曼均值 → 挂谷切空间
        System.out.println("[2/6] 各频段挂谷切空间投影...");
        List<List<double[]>> allBandFeats = new ArrayList<>();
        bandRefs = new RealMatrix[numBands];

        for (int b = 0; b < numBands; b++) {
            double lo = FREQ_BANDS[b][0], hi = FREQ_BANDS[b][1];
            System.out.printf("  频段 %d/%d: %.0f-%.0f Hz%n", b + 1, numBands, lo, hi);

            List<double[][]> filtered = new ArrayList<>();
            for (Trial t : trials)
                filtered.add(SignalProcessor.bandpassFilterTrial(t, lo, hi));

            List<RealMatrix> covs = new ArrayList<>();
            for (double[][] d : filtered)
                covs.add(RiemannianGeometry.computeCovariance(d));

            bandRefs[b] = RiemannianGeometry.riemannianMean(covs);

            List<double[]> kakeyaVecs = new ArrayList<>();
            for (RealMatrix C : covs)
                kakeyaVecs.add(KakeyaGeometry.kakeyaTangentFeatures(C, bandRefs[b], KAKEYA_K));

            allBandFeats.add(kakeyaVecs);
        }

        // [3] 挂谷重叠度分析 (诊断)
        System.out.println("\n[3/6] 挂谷方向重叠度分析 (每频段各类别对间)...");
        for (int b = 0; b < numBands; b++) {
            double lo = FREQ_BANDS[b][0], hi = FREQ_BANDS[b][1];
            System.out.printf("  %.0f-%.0f Hz:", lo, hi);
            for (int i = 0; i < ALL_CLASSES.length; i++) {
                for (int j = i + 1; j < ALL_CLASSES.length; j++) {
                    int ca = ALL_CLASSES[i], cb = ALL_CLASSES[j];
                    List<RealMatrix> covsA = classCovs(trials, classMap, b, ca);
                    List<RealMatrix> covsB = classCovs(trials, classMap, b, cb);
                    double ov = KakeyaGeometry.kakeyaOverlap(covsA, covsB, bandRefs[b], KAKEYA_K);
                    System.out.printf(" %s-%s=%.3f", className(ca).substring(0, 2), className(cb).substring(0, 2), ov);
                }
            }
            System.out.println();
        }

        // [4] 拼接 + Z-score
        System.out.println("\n[4/6] 拼接挂谷特征 + Z-score...");
        int concatDim = numBands * kakeyaDim;
        standardizePerBand(allBandFeats);

        List<double[]> concatFeats = new ArrayList<>();
        List<Integer> allLabels = new ArrayList<>();
        for (int i = 0; i < trials.size(); i++) {
            double[] cat = new double[concatDim];
            int off = 0;
            for (int b = 0; b < numBands; b++) {
                double[] bf = allBandFeats.get(b).get(i);
                System.arraycopy(bf, 0, cat, off, kakeyaDim);
                off += kakeyaDim;
            }
            concatFeats.add(cat);
            allLabels.add(trials.get(i).label);
        }
        standardizeGlobal(concatFeats);
        System.out.printf("  拼接维度: %d (vs 完整切空间 1265)%n", concatDim);

        // [5] ANOVA F-score
        System.out.println("[5/6] 挂谷特征 ANOVA F-score 选择...");
        int keep = Math.min(FS_KEEP, concatDim);
        selectedIndices = FeatureSelector.selectTopKMulti(concatFeats, allLabels, keep);
        List<double[]> reduced = FeatureSelector.applySelectionBatch(concatFeats, selectedIndices);
        System.out.printf("  降维: %d → %d  (样本/特征比 = %.2f)%n",
                concatDim, reduced.get(0).length, (double) trials.size() / reduced.get(0).length);

        // [6] Softmax
        System.out.println("[6/6] 训练 Softmax...");
        List<Integer> labelIndices = new ArrayList<>();
        for (int lb : allLabels) labelIndices.add(labelToIndex(lb));
        softmax = new SoftmaxRegression();
        softmax.setMaxEpochs(800);
        softmax.setLearningRate(0.005);
        softmax.setL2Lambda(0.005);
        softmax.train(reduced, labelIndices, 4);

        trained = true;
        System.out.println("\n挂谷-FB-RG 管线 v8 训练完成！");
    }

    // ==================== 预测 ====================

    public int predict(Trial trial) {
        if (!trained) throw new IllegalStateException("管线未训练");

        Trial copy = deepCopy(trial);
        SignalProcessor.applyCAR(copy);

        int concatDim = numBands * kakeyaDim;
        double[] cat = new double[concatDim];

        for (int b = 0; b < numBands; b++) {
            double lo = FREQ_BANDS[b][0], hi = FREQ_BANDS[b][1];
            double[][] filtered = SignalProcessor.bandpassFilterTrial(copy, lo, hi);
            RealMatrix C = RiemannianGeometry.computeCovariance(filtered);
            double[] kf = KakeyaGeometry.kakeyaTangentFeatures(C, bandRefs[b], KAKEYA_K);

            for (int d = 0; d < kakeyaDim; d++)
                kf[d] = (kf[d] - bandMean[b][d]) / bandStd[b][d];

            int off = b * kakeyaDim;
            System.arraycopy(kf, 0, cat, off, kakeyaDim);
        }

        for (int d = 0; d < concatDim; d++)
            cat[d] = (cat[d] - globalMean[d]) / globalStd[d];

        double[] reduced = FeatureSelector.applySelection(cat, selectedIndices);
        return ALL_CLASSES[softmax.predict(reduced)];
    }

    // ==================== 标准化 ====================

    private void standardizePerBand(List<List<double[]>> allBandFeats) {
        bandMean = new double[numBands][kakeyaDim];
        bandStd  = new double[numBands][kakeyaDim];

        for (int b = 0; b < numBands; b++) {
            List<double[]> feats = allBandFeats.get(b);
            int N = feats.size();
            for (double[] f : feats) for (int d = 0; d < kakeyaDim; d++) bandMean[b][d] += f[d];
            for (int d = 0; d < kakeyaDim; d++) bandMean[b][d] /= N;
            for (double[] f : feats) for (int d = 0; d < kakeyaDim; d++)
                bandStd[b][d] += Math.pow(f[d] - bandMean[b][d], 2);
            for (int d = 0; d < kakeyaDim; d++) {
                bandStd[b][d] = Math.sqrt(bandStd[b][d] / N);
                if (bandStd[b][d] < 1e-10) bandStd[b][d] = 1.0;
            }
            for (double[] f : feats)
                for (int d = 0; d < kakeyaDim; d++)
                    f[d] = (f[d] - bandMean[b][d]) / bandStd[b][d];
        }
    }

    private void standardizeGlobal(List<double[]> feats) {
        int N = feats.size(), D = feats.get(0).length;
        globalMean = new double[D]; globalStd = new double[D];
        for (double[] f : feats) for (int d = 0; d < D; d++) globalMean[d] += f[d];
        for (int d = 0; d < D; d++) globalMean[d] /= N;
        for (double[] f : feats) for (int d = 0; d < D; d++)
            globalStd[d] += Math.pow(f[d] - globalMean[d], 2);
        for (int d = 0; d < D; d++) {
            globalStd[d] = Math.sqrt(globalStd[d] / N);
            if (globalStd[d] < 1e-10) globalStd[d] = 1.0;
        }
        for (double[] f : feats)
            for (int d = 0; d < D; d++)
                f[d] = (f[d] - globalMean[d]) / globalStd[d];
    }

    // ==================== 辅助 ====================

    private List<RealMatrix> classCovs(List<Trial> all, Map<Integer, List<Trial>> classMap,
                                        int band, int cls) {
        double lo = FREQ_BANDS[band][0], hi = FREQ_BANDS[band][1];
        List<RealMatrix> list = new ArrayList<>();
        for (Trial t : classMap.get(cls)) {
            double[][] filt = SignalProcessor.bandpassFilterTrial(t, lo, hi);
            list.add(RiemannianGeometry.computeCovariance(filt));
        }
        return list;
    }

    private int labelToIndex(int label) {
        for (int i = 0; i < ALL_CLASSES.length; i++)
            if (ALL_CLASSES[i] == label) return i;
        return -1;
    }

    public static String className(int label) {
        for (int i = 0; i < ALL_CLASSES.length; i++)
            if (ALL_CLASSES[i] == label) return CLASS_NAMES[i];
        return "?";
    }

    private Trial deepCopy(Trial t) {
        Trial c = new Trial(t.data.length, t.data[0].length);
        c.label = t.label;
        for (int ch = 0; ch < t.data.length; ch++)
            System.arraycopy(t.data[ch], 0, c.data[ch], 0, t.data[ch].length);
        return c;
    }
}
