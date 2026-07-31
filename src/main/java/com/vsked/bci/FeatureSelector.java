package com.vsked.bci;

import java.util.*;

/**
 * 基于 Fisher Score (ANOVA F-statistic) 的特征选择器
 *
 * 支持二分类和多分类：多分类使用 one-vs-rest 平均
 */
public class FeatureSelector {

    /**
     * 多分类 ANOVA F-score 特征选择
     * score(f) = between_class_variance / within_class_variance
     * between = Σ_c n_c * (μ_c - μ)^2
     * within  = Σ_c Σ_{i in c} (x_i - μ_c)^2
     */
    public static int[] selectTopKMulti(List<double[]> features, List<Integer> labels, int k) {
        int numFeatures = features.get(0).length;
        int N = features.size();

        // 收集所有出现的类别
        Set<Integer> classSet = new LinkedHashSet<>(labels);
        List<Integer> classList = new ArrayList<>(classSet);
        int numClasses = classList.size();

        if (numClasses <= 1) {
            // 退化为取前 k 个方差最大的特征
            return selectByVariance(features, k);
        }

        // 按类别分组
        Map<Integer, List<double[]>> groups = new LinkedHashMap<>();
        Map<Integer, int[]> groupIndices = new LinkedHashMap<>();
        for (int cls : classList) groups.put(cls, new ArrayList<>());
        for (int i = 0; i < N; i++) groups.get(labels.get(i)).add(features.get(i));

        // 计算每个特征的 ANOVA F-score
        Score[] scores = new Score[numFeatures];
        for (int f = 0; f < numFeatures; f++) {
            // 每类均值 + 全局均值
            double globalMean = 0;
            double[] classMeans = new double[numClasses];
            int[] classCounts = new int[numClasses];

            for (int c = 0; c < numClasses; c++) {
                int cls = classList.get(c);
                List<double[]> group = groups.get(cls);
                classCounts[c] = group.size();
                double sum = 0;
                for (double[] feats : group) sum += feats[f];
                classMeans[c] = sum / classCounts[c];
                globalMean += sum;
            }
            globalMean /= N;

            // between-class variance
            double between = 0;
            for (int c = 0; c < numClasses; c++) {
                double diff = classMeans[c] - globalMean;
                between += classCounts[c] * diff * diff;
            }
            between /= (numClasses - 1);

            // within-class variance
            double within = 0;
            for (int c = 0; c < numClasses; c++) {
                int cls = classList.get(c);
                for (double[] feats : groups.get(cls)) {
                    double diff = feats[f] - classMeans[c];
                    within += diff * diff;
                }
            }
            within /= (N - numClasses);

            double score = (within > 1e-12) ? between / within : 0;
            scores[f] = new Score(f, score);
        }

        Arrays.sort(scores, (a, b) -> Double.compare(b.value, a.value));

        int[] selected = new int[Math.min(k, numFeatures)];
        for (int i = 0; i < selected.length; i++) selected[i] = scores[i].index;

        System.out.printf("特征选择: 从 %d 维中选出 top-%d, 最高 F=%.4f, 最低=%.4f%n",
                numFeatures, selected.length, scores[0].value, scores[selected.length - 1].value);

        return selected;
    }

    /**
     * 从完整特征中提取被选中的维度
     */
    public static double[] applySelection(double[] fullFeatures, int[] selectedIndices) {
        double[] result = new double[selectedIndices.length];
        for (int i = 0; i < selectedIndices.length; i++) {
            result[i] = fullFeatures[selectedIndices[i]];
        }
        return result;
    }

    /**
     * 对所有特征列表批量应用选择
     */
    public static List<double[]> applySelectionBatch(List<double[]> features, int[] selectedIndices) {
        List<double[]> result = new ArrayList<>();
        for (double[] f : features) {
            result.add(applySelection(f, selectedIndices));
        }
        return result;
    }

    private static int[] selectByVariance(List<double[]> features, int k) {
        int D = features.get(0).length;
        int N = features.size();
        Score[] scores = new Score[D];

        double[] means = new double[D];
        for (double[] f : features)
            for (int d = 0; d < D; d++) means[d] += f[d];
        for (int d = 0; d < D; d++) means[d] /= N;

        for (int d = 0; d < D; d++) {
            double var = 0;
            for (double[] f : features) var += Math.pow(f[d] - means[d], 2);
            scores[d] = new Score(d, var / N);
        }
        Arrays.sort(scores, (a, b) -> Double.compare(b.value, a.value));

        int[] selected = new int[Math.min(k, D)];
        for (int i = 0; i < selected.length; i++) selected[i] = scores[i].index;
        return selected;
    }

    private static class Score {
        int index;
        double value;

        Score(int index, double value) {
            this.index = index;
            this.value = value;
        }
    }
}
