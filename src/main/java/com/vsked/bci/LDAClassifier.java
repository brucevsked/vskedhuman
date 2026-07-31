package com.vsked.bci;

import org.apache.commons.math3.linear.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 二分类 LDA：自适应正则化
 * 训练时自动记住两个类别标签，预测时无需传入。
 */
public class LDAClassifier {

    private RealVector weights;
    private double threshold;
    private int labelA; // 训练时 clA（投影值高的一侧）的标签
    private int labelB; // 训练时 clB 的标签

    /**
     * 用特征向量和标签训练。自动按标签分成两类并记住顺序。
     */
    public void train(List<double[]> features, List<Integer> labels) {
        // 确定两个类别
        this.labelA = labels.get(0);
        this.labelB = labels.get(0);
        for (int lb : labels) {
            if (lb != labelA) { labelB = lb; break; }
        }
        if (labelA == labelB) {
            System.err.printf("LDA 训练失败：数据只有一类 (%d)%n", labelA);
            return;
        }

        List<RealVector> clA = new ArrayList<>();
        List<RealVector> clB = new ArrayList<>();

        for (int i = 0; i < features.size(); i++) {
            RealVector f = new ArrayRealVector(features.get(i));
            if (labels.get(i) == labelA) clA.add(f);
            else clB.add(f);
        }

        if (clA.isEmpty() || clB.isEmpty()) {
            System.err.printf("LDA: 某类无样本 (A=%d: %d, B=%d: %d)%n",
                labelA, clA.size(), labelB, clB.size());
            return;
        }

        int dim = clA.get(0).getDimension();
        RealVector muA = mean(clA);
        RealVector muB = mean(clB);
        RealMatrix Sw = scatter(clA, muA).add(scatter(clB, muB));

        double traceSw = 0;
        for (int i = 0; i < dim; i++) traceSw += Sw.getEntry(i, i);
        double lambda = Math.max(traceSw / dim * 1e-3, 1e-8);

        RealMatrix I = MatrixUtils.createRealIdentityMatrix(dim);
        RealMatrix SwReg = Sw.add(I.scalarMultiply(lambda));
        RealVector diff = muA.subtract(muB); // A均值 - B均值

        RealMatrix SwInv;
        try {
            SwInv = new LUDecomposition(SwReg).getSolver().getInverse();
        } catch (SingularMatrixException e) {
            SwReg = Sw.add(I.scalarMultiply(lambda * 100));
            try {
                SwInv = new LUDecomposition(SwReg).getSolver().getInverse();
            } catch (SingularMatrixException e2) {
                SwInv = new SingularValueDecomposition(SwReg).getSolver().getInverse();
            }
        }

        this.weights = SwInv.operate(diff);
        // 阈值 = w · (μA + μB) / 2
        this.threshold = weights.dotProduct(muA.add(muB).mapMultiply(0.5));

        System.out.printf("    LDA: A=%d(%d样本) B=%d(%d样本) dim=%d%n",
            labelA, clA.size(), labelB, clB.size(), dim);
    }

    /**
     * 预测：返回训练时记住的 labelA 或 labelB
     */
    public int predict(double[] features) {
        double proj = rawProjection(features);
        return (proj > threshold) ? labelA : labelB;
    }

    /**
     * 到决策边界的距离（正数），用于置信度估计
     */
    public double projectionDistance(double[] features) {
        return Math.abs(rawProjection(features) - threshold);
    }

    private double rawProjection(double[] features) {
        RealVector feat = new ArrayRealVector(features);
        return weights.dotProduct(feat);
    }

    private RealVector mean(List<RealVector> list) {
        RealVector sum = new ArrayRealVector(list.get(0).getDimension());
        for (RealVector v : list) sum = sum.add(v);
        return sum.mapDivide(list.size());
    }

    private RealMatrix scatter(List<RealVector> list, RealVector mu) {
        int dim = mu.getDimension();
        RealMatrix mat = MatrixUtils.createRealMatrix(dim, dim);
        for (RealVector v : list) {
            RealVector d = v.subtract(mu);
            mat = mat.add(d.outerProduct(d));
        }
        return mat;
    }
}
