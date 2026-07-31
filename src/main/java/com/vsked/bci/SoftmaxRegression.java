package com.vsked.bci;

import org.apache.commons.math3.linear.*;
import java.util.*;

/**
 * 多分类 Softmax 回归 (SGD + momentum + early stopping)
 *
 * K 类 softmax: P(y=k|x) = exp(w_k·x + b_k) / Σ_j exp(w_j·x + b_j)
 * 损失: cross-entropy + L2 正则化
 */
public class SoftmaxRegression {

    private RealMatrix W;  // D × K
    private RealVector b;  // K

    private double learningRate = 0.01;
    private double l2Lambda = 0.001;
    private int maxEpochs = 500;
    private int batchSize = 32;
    private double momentum = 0.9;
    private int evalInterval = 50;
    private int patience = 30;

    public void setLearningRate(double lr) { this.learningRate = lr; }
    public void setL2Lambda(double l2) { this.l2Lambda = l2; }
    public void setMaxEpochs(int e) { this.maxEpochs = e; }

    /**
     * 训练
     * @param features  N × D
     * @param labels    N 个类别标签（0-based index）
     * @param numClasses K
     */
    public void train(List<double[]> features, List<Integer> labels, int numClasses) {
        int N = features.size();
        int D = features.get(0).length;
        int K = numClasses;
        Random rng = new Random(42);

        // Xavier 初始化
        W = MatrixUtils.createRealMatrix(D, K);
        b = new ArrayRealVector(K);
        double scale = Math.sqrt(2.0 / D);
        for (int d = 0; d < D; d++) {
            for (int k = 0; k < K; k++) {
                W.setEntry(d, k, rng.nextGaussian() * scale);
            }
        }

        // 转为矩阵批量运算
        RealMatrix Xmat = MatrixUtils.createRealMatrix(N, D);
        for (int i = 0; i < N; i++) {
            double[] f = features.get(i);
            for (int d = 0; d < D; d++) Xmat.setEntry(i, d, f[d]);
        }

        System.out.printf("  Softmax: %d 样本, %d 维, %d 类, epochs=%d, lr=%.4f, l2=%.4f%n",
                N, D, K, maxEpochs, learningRate, l2Lambda);

        // Momentum 速度矩阵
        RealMatrix vW = MatrixUtils.createRealMatrix(D, K);
        RealVector vb = new ArrayRealVector(K);

        double bestLoss = Double.MAX_VALUE;
        int noImprove = 0;

        for (int epoch = 0; epoch < maxEpochs; epoch++) {
            // 打乱批次顺序
            int[] indices = shuffledIndices(N, rng);

            for (int start = 0; start < N; start += batchSize) {
                int end = Math.min(start + batchSize, N);
                int M = end - start;

                // 构造 mini-batch 矩阵
                RealMatrix Xb = MatrixUtils.createRealMatrix(M, D);
                int[] batchLabels = new int[M];
                for (int i = 0; i < M; i++) {
                    int idx = indices[start + i];
                    Xb.setRowVector(i, Xmat.getRowVector(idx));
                    batchLabels[i] = labels.get(idx);
                }

                // scores = Xb * W + b → M×K
                RealMatrix scores = Xb.multiply(W);
                for (int i = 0; i < M; i++) {
                    RealVector row = scores.getRowVector(i);
                    scores.setRowVector(i, row.add(b));
                }

                // softmax
                RealMatrix probs = softmax(scores);

                // 梯度: dScores = (probs - Y_onehot) / M
                RealMatrix dScores = probs.copy();
                for (int i = 0; i < M; i++) {
                    for (int k = 0; k < K; k++) {
                        double target = (k == batchLabels[i]) ? 1.0 : 0.0;
                        dScores.setEntry(i, k, (probs.getEntry(i, k) - target) / M);
                    }
                }

                // dW = Xb^T * dScores + l2 * W
                RealMatrix dW = Xb.transpose().multiply(dScores)
                        .add(W.scalarMultiply(l2Lambda));

                // db = mean(dScores per row) — sum over batch dimension
                RealVector db = new ArrayRealVector(K);
                for (int i = 0; i < M; i++) {
                    db = db.add(dScores.getRowVector(i));
                }

                // Momentum 更新
                vW = vW.scalarMultiply(momentum).subtract(dW.scalarMultiply(learningRate));
                vb = vb.mapMultiply(momentum).subtract(db.mapMultiply(learningRate));

                W = W.add(vW);
                b = b.add(vb);
            }

            // 定期评估 loss
            if (epoch % evalInterval == 0 || epoch == maxEpochs - 1) {
                double loss = computeLoss(Xmat, labels, N, K);
                if (epoch % 100 == 0)
                    System.out.printf("  epoch %d/%d  loss=%.6f%n", epoch, maxEpochs, loss);

                if (loss < bestLoss - 1e-6) {
                    bestLoss = loss;
                    noImprove = 0;
                } else {
                    noImprove++;
                }
                if (noImprove > patience) {
                    System.out.printf("  early stopping @ epoch %d (loss=%.6f)%n", epoch, loss);
                    break;
                }
            }
        }
        System.out.printf("  Softmax 训练完成, 最终 loss=%.6f%n", bestLoss);
    }

    /**
     * 预测：返回概率最高的类别 index (0-based)
     */
    public int predict(double[] features) {
        int K = W.getColumnDimension();
        RealVector x = new ArrayRealVector(features);
        RealVector scores = W.preMultiply(x).add(b);

        int maxK = 0;
        double maxScore = scores.getEntry(0);
        for (int k = 1; k < K; k++) {
            if (scores.getEntry(k) > maxScore) {
                maxScore = scores.getEntry(k);
                maxK = k;
            }
        }
        return maxK;
    }

    // ==================== 内部方法 ====================

    private RealMatrix softmax(RealMatrix scores) {
        int rows = scores.getRowDimension();
        int cols = scores.getColumnDimension();
        RealMatrix probs = MatrixUtils.createRealMatrix(rows, cols);

        for (int i = 0; i < rows; i++) {
            double maxVal = scores.getEntry(i, 0);
            for (int j = 1; j < cols; j++) {
                maxVal = Math.max(maxVal, scores.getEntry(i, j));
            }
            double sum = 0;
            for (int j = 0; j < cols; j++) {
                double v = Math.exp(scores.getEntry(i, j) - maxVal);
                probs.setEntry(i, j, v);
                sum += v;
            }
            for (int j = 0; j < cols; j++) {
                probs.setEntry(i, j, probs.getEntry(i, j) / sum);
            }
        }
        return probs;
    }

    private double computeLoss(RealMatrix X, List<Integer> labels, int N, int K) {
        RealMatrix scores = X.multiply(W);
        for (int i = 0; i < N; i++) {
            RealVector row = scores.getRowVector(i);
            scores.setRowVector(i, row.add(b));
        }
        RealMatrix probs = softmax(scores);

        double ce = 0;
        for (int i = 0; i < N; i++) {
            ce -= Math.log(Math.max(probs.getEntry(i, labels.get(i)), 1e-15));
        }
        ce /= N;

        double l2 = 0;
        for (int d = 0; d < W.getRowDimension(); d++) {
            for (int k = 0; k < K; k++) {
                l2 += W.getEntry(d, k) * W.getEntry(d, k);
            }
        }
        l2 *= 0.5 * l2Lambda;
        return ce + l2;
    }

    private int[] shuffledIndices(int N, Random rng) {
        int[] idx = new int[N];
        for (int i = 0; i < N; i++) idx[i] = i;
        for (int i = N - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = idx[i];
            idx[i] = idx[j];
            idx[j] = tmp;
        }
        return idx;
    }
}
