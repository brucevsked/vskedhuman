package com.vsked.bci;

import java.util.*;

/**
 * Bagging 集成分类器
 * 训练多个 LDA 模型（Bootstrap样本 + 随机特征子空间），多数投票决策
 * 极大降低单模型方差，提升小样本场景下的泛化能力
 */
public class BaggedEnsemble {

    private final int numModels;           // 集成模型数量
    private final double featureRatio;     // 每次随机选取的特征比例
    private final List<LDAClassifier> models;
    private final List<int[]> modelFeatures; // 每个模型使用的特征索引
    private final Random rng;

    private boolean trained = false;

    public BaggedEnsemble() {
        this(51, 0.70); // 51个模型，每个用70%特征
    }

    public BaggedEnsemble(int numModels, double featureRatio) {
        this.numModels = numModels;
        this.featureRatio = featureRatio;
        this.models = new ArrayList<>();
        this.modelFeatures = new ArrayList<>();
        this.rng = new Random(42); // 固定种子可复现
    }

    /**
     * 训练集成模型
     */
    public void train(List<double[]> featuresList, List<Integer> labels) {
        int n = featuresList.size();
        int dim = featuresList.get(0).length;
        int featureSubsetSize = Math.max(dim / 2, (int) (dim * featureRatio));

        models.clear();
        modelFeatures.clear();

        System.out.printf("Bagging 集成训练 (%d 个模型, %.0f%% 特征)...%n",
            numModels, featureRatio * 100);

        for (int m = 0; m < numModels; m++) {
            // 1. Bootstrap 采样（有放回）
            List<double[]> bootFeats = new ArrayList<>();
            List<Integer> bootLabels = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int idx = rng.nextInt(n);
                bootFeats.add(featuresList.get(idx));
                bootLabels.add(labels.get(idx));
            }

            // 2. 随机选择特征子集
            int[] selectedFeatures = randomSubset(dim, featureSubsetSize);
            modelFeatures.add(selectedFeatures);

            // 3. 提取子特征
            List<double[]> subFeatures = new ArrayList<>();
            for (double[] full : bootFeats) {
                double[] sub = new double[featureSubsetSize];
                for (int j = 0; j < featureSubsetSize; j++) {
                    sub[j] = full[selectedFeatures[j]];
                }
                subFeatures.add(sub);
            }

            // 4. 训练 LDA
            LDAClassifier model = new LDAClassifier();
            model.train(subFeatures, bootLabels);
            models.add(model);
        }

        trained = true;
        System.out.println("集成训练完成！");
    }

    /**
     * 预测（多数投票）
     */
    public int predict(double[] fullFeatures) {
        if (!trained) throw new IllegalStateException("集成模型尚未训练");

        int leftVotes = 0, rightVotes = 0;

        for (int m = 0; m < models.size(); m++) {
            int[] featIndices = modelFeatures.get(m);
            double[] subFeat = new double[featIndices.length];
            for (int j = 0; j < featIndices.length; j++) {
                subFeat[j] = fullFeatures[featIndices[j]];
            }

            int pred = models.get(m).predict(subFeat);
            if (pred == 769) leftVotes++;
            else rightVotes++;
        }

        return leftVotes > rightVotes ? 769 : 770;
    }

    private int[] randomSubset(int total, int size) {
        // Fisher-Yates 部分洗牌
        int[] arr = new int[total];
        for (int i = 0; i < total; i++) arr[i] = i;
        for (int i = 0; i < size; i++) {
            int j = i + rng.nextInt(total - i);
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return Arrays.copyOf(arr, size);
    }
}
