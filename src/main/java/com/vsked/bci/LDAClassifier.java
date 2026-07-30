package com.vsked.bci;

import org.apache.commons.math3.linear.*;
import java.util.ArrayList;
import java.util.List;

/**
 * LDA 分类器：自适应正则化
 */
public class LDAClassifier {

    private RealVector weights;
    private double threshold; // 769 if projection > threshold, else 770

    /**
     * 用预提取的特征向量和标签训练
     */
    public void train(List<double[]> featuresList, List<Integer> labels) {
        List<RealVector> classLeft = new ArrayList<>();
        List<RealVector> classRight = new ArrayList<>();

        for (int i = 0; i < featuresList.size(); i++) {
            RealVector f = new ArrayRealVector(featuresList.get(i));
            if (labels.get(i) == 769) classLeft.add(f);
            else classRight.add(f);
        }

        if (classLeft.isEmpty() || classRight.isEmpty()) {
            System.err.println("LDA 训练失败：某一类没有样本");
            return;
        }

        int dim = classLeft.get(0).getDimension();
        System.out.printf("  LDA: 左手=%d, 右手=%d, 维度=%d%n",
            classLeft.size(), classRight.size(), dim);

        RealVector mu1 = computeMean(classLeft);
        RealVector mu2 = computeMean(classRight);

        RealMatrix Sw = computeScatterMatrix(classLeft, mu1)
            .add(computeScatterMatrix(classRight, mu2));

        // 自适应正则化: λ = trace(Sw) / dim * 1e-3
        double traceSw = 0;
        for (int i = 0; i < dim; i++) traceSw += Sw.getEntry(i, i);
        double lambda = Math.max(traceSw / dim * 1e-3, 1e-8);

        RealMatrix I = MatrixUtils.createRealIdentityMatrix(dim);
        RealMatrix SwReg = Sw.add(I.scalarMultiply(lambda));

        RealVector diff = mu1.subtract(mu2);

        RealMatrix SwInv;
        try {
            SwInv = new LUDecomposition(SwReg).getSolver().getInverse();
        } catch (SingularMatrixException e) {
            // 降级：增大 λ
            SwReg = Sw.add(I.scalarMultiply(lambda * 100));
            try {
                SwInv = new LUDecomposition(SwReg).getSolver().getInverse();
            } catch (SingularMatrixException e2) {
                // 最终降级：SVD 伪逆
                SwInv = new SingularValueDecomposition(SwReg).getSolver().getInverse();
            }
        }

        this.weights = SwInv.operate(diff);
        this.threshold = weights.dotProduct(mu1.add(mu2).mapMultiply(0.5));

        System.out.printf("  LDA 完成, λ=%.6f, ||w||=%.4f%n", lambda, weights.getNorm());
    }

    /**
     * 用特征向量预测
     */
    public int predict(double[] features) {
        RealVector feat = new ArrayRealVector(features);
        double projection = weights.dotProduct(feat);
        return (projection > threshold) ? 769 : 770;
    }

    private RealVector computeMean(List<RealVector> list) {
        RealVector sum = new ArrayRealVector(list.get(0).getDimension());
        for (RealVector v : list) sum = sum.add(v);
        return sum.mapDivide(list.size());
    }

    private RealMatrix computeScatterMatrix(List<RealVector> list, RealVector mu) {
        int dim = mu.getDimension();
        RealMatrix matrix = MatrixUtils.createRealMatrix(dim, dim);
        for (RealVector v : list) {
            RealVector diff = v.subtract(mu);
            matrix = matrix.add(diff.outerProduct(diff));
        }
        return matrix;
    }
}
