package com.vsked.bci;

import org.apache.commons.math3.linear.*;
import java.util.*;

/**
 * 滤波器组共空间模式 (Filter Bank Common Spatial Patterns)
 * 8 频段 × 3 对 CSP = 48 维原始特征
 */
public class FBCCSPFilter {

    private final int numCSPPairs;
    private RealMatrix[] cspFilters;
    private double[][] freqBands;

    public FBCCSPFilter(int numCSPPairs) {
        this.numCSPPairs = numCSPPairs;
    }

    public void fit(List<Trial> trials, double[][] bands) {
        this.freqBands = bands;
        this.cspFilters = new RealMatrix[bands.length];

        for (int b = 0; b < bands.length; b++) {
            double low = bands[b][0];
            double high = bands[b][1];
            System.out.printf("  频段 %d: %.0f-%.0f Hz — 训练 CSP...%n", b + 1, low, high);

            List<double[][]> bandLeft = new ArrayList<>();
            List<double[][]> bandRight = new ArrayList<>();

            for (Trial t : trials) {
                double[][] filtered = SignalProcessor.bandpassFilterTrial(t, low, high);
                if (t.label == 769) bandLeft.add(filtered);
                else if (t.label == 770) bandRight.add(filtered);
            }

            if (bandLeft.isEmpty() || bandRight.isEmpty()) {
                System.out.printf("  警告: 频段 %d 某类样本为空，跳过%n", b + 1);
                continue;
            }
            cspFilters[b] = trainCSP(bandLeft, bandRight);
        }
        int validBands = (int) Arrays.stream(cspFilters).filter(Objects::nonNull).count();
        System.out.printf("CSP 训练完成：%d 频段 × %d 对 = %d 维原始特征%n",
            validBands, numCSPPairs, validBands * numCSPPairs * 2);
    }

    public double[] extractFeatures(Trial trial) {
        List<Double> allFeatures = new ArrayList<>();
        for (int b = 0; b < freqBands.length; b++) {
            if (cspFilters[b] == null) continue;
            double[][] filtered = SignalProcessor.bandpassFilterTrial(trial,
                freqBands[b][0], freqBands[b][1]);
            double[][] cspOut = applyCSP(filtered, cspFilters[b]);
            for (int ch = 0; ch < cspOut.length; ch++) {
                double v = computeVariance(cspOut[ch]);
                allFeatures.add(Math.log(Math.max(v, 1e-10)));
            }
        }
        double[] result = new double[allFeatures.size()];
        for (int i = 0; i < result.length; i++) result[i] = allFeatures.get(i);
        return result;
    }

    // ============ 标准 CSP ============

    private RealMatrix trainCSP(List<double[][]> classLeft, List<double[][]> classRight) {
        int channels = classLeft.get(0).length;
        int n = channels;

        RealMatrix covLeft = avgCov(classLeft);
        RealMatrix covRight = avgCov(classRight);
        RealMatrix R = covLeft.add(covRight);

        // 对 R 做特征分解: R = U * D * U^T
        EigenDecomposition eigR = new EigenDecomposition(R);
        RealMatrix U = eigR.getV();
        double[] ev = eigR.getRealEigenvalues();

        // 白化矩阵: P = D^(-1/2) * U^T
        double[] diagInvSqrt = new double[n];
        for (int i = 0; i < n; i++) {
            diagInvSqrt[i] = 1.0 / Math.sqrt(Math.max(ev[i], 1e-8));
        }
        RealMatrix Dinv = MatrixUtils.createRealDiagonalMatrix(diagInvSqrt);
        RealMatrix P = Dinv.multiply(U.transpose());

        // S1 = P * covLeft * P^T
        RealMatrix S1 = P.multiply(covLeft).multiply(P.transpose());

        // S1 的特征分解
        EigenDecomposition eigS1 = new EigenDecomposition(S1);
        RealMatrix B = eigS1.getV();
        double[] lambda = eigS1.getRealEigenvalues();

        // 完整 CSP 投影: WFull = B^T * P
        RealMatrix WFull = B.transpose().multiply(P);

        // 按特征值降序排列，取前 numCSPPairs 和后 numCSPPairs
        int dim = lambda.length;
        Integer[] idx = new Integer[dim];
        for (int i = 0; i < dim; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(lambda[b], lambda[a]));

        int numFilters = Math.min(numCSPPairs * 2, dim);
        RealMatrix W = MatrixUtils.createRealMatrix(numFilters, dim);

        // 前 numCSPPairs 行 = 最大特征值对应的方向（classLeft 方差最大）
        for (int i = 0; i < numCSPPairs && i < numFilters; i++) {
            W.setRowVector(i, WFull.getRowVector(idx[i]));
        }
        // 后 numCSPPairs 行 = 最小特征值对应的方向（classRight 方差最大）
        for (int i = 0; i < numCSPPairs && (numCSPPairs + i) < numFilters; i++) {
            W.setRowVector(numCSPPairs + i, WFull.getRowVector(idx[dim - 1 - i]));
        }
        return W;
    }

    private double[][] applyCSP(double[][] data, RealMatrix W) {
        int samples = data[0].length;
        int outCh = W.getRowDimension();
        double[][] result = new double[outCh][samples];
        for (int t = 0; t < samples; t++) {
            RealVector x = new ArrayRealVector(data.length);
            for (int ch = 0; ch < data.length; ch++) x.setEntry(ch, data[ch][t]);
            RealVector y = W.operate(x);
            for (int ch = 0; ch < outCh; ch++) result[ch][t] = y.getEntry(ch);
        }
        return result;
    }

    private RealMatrix avgCov(List<double[][]> trials) {
        int channels = trials.get(0).length;
        int samples = trials.get(0)[0].length;
        RealMatrix sum = MatrixUtils.createRealMatrix(channels, channels);
        for (double[][] data : trials) {
            RealMatrix X = MatrixUtils.createRealMatrix(channels, samples);
            for (int ch = 0; ch < channels; ch++) {
                double m = 0;
                for (int t = 0; t < samples; t++) m += data[ch][t];
                m /= samples;
                for (int t = 0; t < samples; t++) X.setEntry(ch, t, data[ch][t] - m);
            }
            sum = sum.add(X.multiply(X.transpose()).scalarMultiply(1.0 / (samples - 1)));
        }
        return sum.scalarMultiply(1.0 / trials.size());
    }

    private double computeVariance(double[] arr) {
        double mean = 0, m2 = 0;
        for (int i = 0; i < arr.length; i++) {
            double delta = arr[i] - mean;
            mean += delta / (i + 1);
            m2 += delta * (arr[i] - mean);
        }
        return m2 / (arr.length - 1);
    }
}
