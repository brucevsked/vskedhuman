package com.vsked.bci;

import java.util.*;

/**
 * 基于 Fisher Score 的特征选择器
 * FisherScore(f) = (μ1-μ2)² / (σ1²+σ2²)
 * 选择分离度最高的 top-k 特征
 */
public class FeatureSelector {

    /**
     * 计算所有特征的 Fisher Score 并返回 top-k 索引
     */
    public static int[] selectTopK(List<double[]> features, List<Integer> labels, int k) {
        int numFeatures = features.get(0).length;

        // 按标签分组
        List<double[]> classLeft = new ArrayList<>();
        List<double[]> classRight = new ArrayList<>();

        for (int i = 0; i < features.size(); i++) {
            if (labels.get(i) == 769) classLeft.add(features.get(i));
            else classRight.add(features.get(i));
        }

        // 计算每个特征的 Fisher Score
        FisherScore[] scores = new FisherScore[numFeatures];
        for (int f = 0; f < numFeatures; f++) {
            double mean1 = 0, mean2 = 0;
            for (double[] feats : classLeft) mean1 += feats[f];
            for (double[] feats : classRight) mean2 += feats[f];
            mean1 /= classLeft.size();
            mean2 /= classRight.size();

            double var1 = 0, var2 = 0;
            for (double[] feats : classLeft) var1 += Math.pow(feats[f] - mean1, 2);
            for (double[] feats : classRight) var2 += Math.pow(feats[f] - mean2, 2);
            var1 /= classLeft.size();
            var2 /= classRight.size();

            double score = (var1 + var2) > 1e-12
                ? (mean1 - mean2) * (mean1 - mean2) / (var1 + var2)
                : 0;
            scores[f] = new FisherScore(f, score);
        }

        // 排序取 top-k
        Arrays.sort(scores, (a, b) -> Double.compare(b.score, a.score));

        int[] selected = new int[k];
        for (int i = 0; i < k; i++) selected[i] = scores[i].index;

        // 打印选中的特征信息
        System.out.printf("特征选择: 从 %d 维中选出 top-%d, 最高 Fisher=%.4f, 最低=%.4f%n",
            numFeatures, k, scores[0].score, scores[k - 1].score);

        return selected;
    }

    /**
     * 应用特征选择：从完整特征中提取被选中的维度
     */
    public static double[] applySelection(double[] fullFeatures, int[] selectedIndices) {
        double[] result = new double[selectedIndices.length];
        for (int i = 0; i < selectedIndices.length; i++) {
            result[i] = fullFeatures[selectedIndices[i]];
        }
        return result;
    }

    private static class FisherScore {
        int index;
        double score;

        FisherScore(int index, double score) {
            this.index = index;
            this.score = score;
        }
    }
}
