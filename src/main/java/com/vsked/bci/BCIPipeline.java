package com.vsked.bci;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BCI 处理管线 v3 — 精简稳定版
 *
 * 管线: CAR去噪 → FBCSP(8频段×3对=48维) → Z-score → Fisher选优(top-24) → LDA分类
 *
 * 与 v2 的关键区别：
 * 1. 去掉数据增强（避免高度相关的重复样本破坏 CSP 协方差估计）
 * 2. 去掉 Bagging 集成（小样本下单个强 LDA 比弱模型投票更可靠）
 * 3. 去掉 RCSP（回归经典 CSP，在小样本 BCI 上更稳定）
 * 4. 统一 FFT 长度，解决训练/测试幅度不一致
 */
public class BCIPipeline {

    private static final double[][] FREQ_BANDS = {
        {4, 8}, {8, 12}, {12, 16}, {16, 20},
        {20, 24}, {24, 28}, {28, 32}, {32, 36}
    };

    private static final int CSP_PAIRS = 3;        // 3对/频段 = 48维
    private static final int FEATURES_TO_KEEP = 24; // Fisher 保留一半

    private FBCCSPFilter fbcsp;
    private int[] selectedIndices;
    private LDAClassifier lda;
    private boolean trained;

    // Z-score 参数
    private double[] trainMean;
    private double[] trainStd;

    public void train(List<Trial> trials) {
        System.out.println("=======================================================");
        System.out.println("BCI 管线 v3 — FBCSP + Fisher + LDA");
        System.out.println("=======================================================");

        List<Trial> binaryTrials = trials.stream()
            .filter(t -> t.label == 769 || t.label == 770)
            .collect(Collectors.toList());

        long nLeft = binaryTrials.stream().filter(t -> t.label == 769).count();
        long nRight = binaryTrials.stream().filter(t -> t.label == 770).count();
        System.out.printf("二分类样本: 左手=%d, 右手=%d%n", nLeft, nRight);

        // [1] CAR 去噪
        System.out.println("\n[1/4] CAR 共平均参考去噪...");
        for (Trial t : binaryTrials) {
            SignalProcessor.applyCAR(t);
        }

        // [2] FBCSP 特征提取
        System.out.println("[2/4] FBCSP 训练 + 特征提取...");
        fbcsp = new FBCCSPFilter(CSP_PAIRS);
        fbcsp.fit(binaryTrials, FREQ_BANDS);

        List<double[]> allFeatures = new ArrayList<>();
        List<Integer> allLabels = new ArrayList<>();
        for (Trial t : binaryTrials) {
            allFeatures.add(fbcsp.extractFeatures(t));
            allLabels.add(t.label);
        }

        int rawDim = allFeatures.get(0).length;
        System.out.printf("  原始特征维度: %d%n", rawDim);

        // [3] Z-score + Fisher 特征选择
        System.out.println("[3/4] Z-score 标准化 + Fisher 特征选择...");
        standardize(allFeatures);

        int selectK = Math.min(FEATURES_TO_KEEP, rawDim);
        selectedIndices = FeatureSelector.selectTopK(allFeatures, allLabels, selectK);

        List<double[]> selFeatures = new ArrayList<>();
        for (double[] f : allFeatures) {
            selFeatures.add(FeatureSelector.applySelection(f, selectedIndices));
        }
        System.out.printf("  最终特征维度: %d%n", selectedIndices.length);

        // [4] LDA 训练
        System.out.println("[4/4] LDA 训练...");
        lda = new LDAClassifier();
        lda.train(selFeatures, allLabels);

        trained = true;
        System.out.println("\n管线 v3 训练完成！");
    }

    public int predict(Trial trial) {
        if (!trained) throw new IllegalStateException("管线尚未训练");

        // CAR（直接修改副本）
        Trial copy = deepCopy(trial);
        SignalProcessor.applyCAR(copy);

        // FBCSP 特征
        double[] fullFeats = fbcsp.extractFeatures(copy);

        // Z-score
        for (int i = 0; i < fullFeats.length && i < trainMean.length; i++) {
            fullFeats[i] = (fullFeats[i] - trainMean[i]) / trainStd[i];
        }

        // Fisher 选择
        double[] selFeats = FeatureSelector.applySelection(fullFeats, selectedIndices);

        // LDA 预测
        return lda.predict(selFeats);
    }

    // ==================== Z-score ====================

    private void standardize(List<double[]> features) {
        int dim = features.get(0).length;
        trainMean = new double[dim];
        trainStd = new double[dim];

        for (double[] f : features) {
            for (int i = 0; i < dim; i++) trainMean[i] += f[i];
        }
        for (int i = 0; i < dim; i++) trainMean[i] /= features.size();

        for (double[] f : features) {
            for (int i = 0; i < dim; i++) {
                trainStd[i] += Math.pow(f[i] - trainMean[i], 2);
            }
        }
        for (int i = 0; i < dim; i++) {
            trainStd[i] = Math.sqrt(trainStd[i] / features.size());
            if (trainStd[i] < 1e-10) trainStd[i] = 1.0;
        }

        for (double[] f : features) {
            for (int i = 0; i < dim; i++) {
                f[i] = (f[i] - trainMean[i]) / trainStd[i];
            }
        }
        System.out.printf("  Z-score 标准化: %d 维%n", dim);
    }

    // ==================== 工具方法 ====================

    private Trial deepCopy(Trial trial) {
        Trial copy = new Trial(trial.data.length, trial.data[0].length);
        copy.label = trial.label;
        for (int ch = 0; ch < trial.data.length; ch++) {
            System.arraycopy(trial.data[ch], 0, copy.data[ch], 0, trial.data[ch].length);
        }
        return copy;
    }
}
