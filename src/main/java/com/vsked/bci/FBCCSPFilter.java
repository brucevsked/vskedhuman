package com.vsked.bci;

import org.apache.commons.math3.linear.*;
import java.util.*;

/**
 * 滤波器组共空间模式 — 二分类版本
 * 按指定的两个类别训练 CSP（如 769 vs 770）
 */
public class FBCCSPFilter {

    private final int numCSPPairs;
    private RealMatrix[] cspFilters;
    private double[][] freqBands;

    public FBCCSPFilter(int numCSPPairs) {
        this.numCSPPairs = numCSPPairs;
    }

    /**
     * 在两个类别之间训练 CSP
     * @param labelA 类别A标签（如 769）
     * @param labelB 类别B标签（如 770）
     */
    public void fit(List<Trial> trials, double[][] bands, int labelA, int labelB) {
        this.freqBands = bands;
        this.cspFilters = new RealMatrix[bands.length];

        for (int b = 0; b < bands.length; b++) {
            double low = bands[b][0];
            double high = bands[b][1];

            List<double[][]> bandA = new ArrayList<>();
            List<double[][]> bandB = new ArrayList<>();

            for (Trial t : trials) {
                if (t.label != labelA && t.label != labelB) continue;
                double[][] filtered = SignalProcessor.bandpassFilterTrial(t, low, high);
                if (t.label == labelA) bandA.add(filtered);
                else bandB.add(filtered);
            }

            if (bandA.isEmpty() || bandB.isEmpty()) continue;
            cspFilters[b] = trainCSP(bandA, bandB);
        }
        int valid = (int) Arrays.stream(cspFilters).filter(Objects::nonNull).count();
        System.out.printf("    [%d vs %d] CSP: %d 频段有效, 共 %d 维%n",
            labelA, labelB, valid, valid * numCSPPairs * 2);
    }

    public double[] extractFeatures(Trial trial) {
        List<Double> all = new ArrayList<>();
        for (int b = 0; b < freqBands.length; b++) {
            if (cspFilters[b] == null) continue;
            double[][] filtered = SignalProcessor.bandpassFilterTrial(trial,
                freqBands[b][0], freqBands[b][1]);
            double[][] cspOut = applyCSP(filtered, cspFilters[b]);
            for (int ch = 0; ch < cspOut.length; ch++) {
                all.add(Math.log(Math.max(computeVariance(cspOut[ch]), 1e-10)));
            }
        }
        double[] result = new double[all.size()];
        for (int i = 0; i < result.length; i++) result[i] = all.get(i);
        return result;
    }

    // ============ 标准 CSP ============

    private RealMatrix trainCSP(List<double[][]> classA, List<double[][]> classB) {
        int channels = classA.get(0).length;
        int n = channels;

        RealMatrix covA = avgCov(classA);
        RealMatrix covB = avgCov(classB);
        RealMatrix R = covA.add(covB);

        EigenDecomposition eigR = new EigenDecomposition(R);
        RealMatrix U = eigR.getV();
        double[] ev = eigR.getRealEigenvalues();

        double[] diagInvSqrt = new double[n];
        for (int i = 0; i < n; i++) diagInvSqrt[i] = 1.0 / Math.sqrt(Math.max(ev[i], 1e-8));
        RealMatrix Dinv = MatrixUtils.createRealDiagonalMatrix(diagInvSqrt);
        RealMatrix P = Dinv.multiply(U.transpose());

        RealMatrix S1 = P.multiply(covA).multiply(P.transpose());
        EigenDecomposition eigS1 = new EigenDecomposition(S1);
        RealMatrix B = eigS1.getV();
        double[] lambda = eigS1.getRealEigenvalues();

        RealMatrix WFull = B.transpose().multiply(P);

        int dim = lambda.length;
        Integer[] idx = new Integer[dim];
        for (int i = 0; i < dim; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(lambda[b], lambda[a]));

        int numFilters = Math.min(numCSPPairs * 2, dim);
        RealMatrix W = MatrixUtils.createRealMatrix(numFilters, dim);

        for (int i = 0; i < numCSPPairs && i < numFilters; i++) {
            W.setRowVector(i, WFull.getRowVector(idx[i]));
        }
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
