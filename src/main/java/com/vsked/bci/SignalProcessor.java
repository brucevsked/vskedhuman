package com.vsked.bci;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.*;

/**
 * 信号预处理工具：CAR 去噪 + FFT 频域滤波
 * 所有滤波使用固定 FFT 长度，确保训练/测试特征一致
 */
public class SignalProcessor {

    private static final int SAMPLE_RATE = 250;
    /** 固定 FFT 长度，避免训练/测试因信号长度不同导致幅度差异 */
    private static final int FIXED_FFT_LEN = 1024;

    /**
     * 共平均参考 (CAR)：每个时间点减去所有通道的均值
     */
    public static void applyCAR(Trial trial) {
        double[][] data = trial.data;
        int channels = data.length;
        int samples = data[0].length;

        for (int t = 0; t < samples; t++) {
            double avg = 0;
            for (int ch = 0; ch < channels; ch++) avg += data[ch][t];
            avg /= channels;
            for (int ch = 0; ch < channels; ch++) data[ch][t] -= avg;
        }
    }

    /**
     * FFT 频域带通滤波：零相位 + 统一 FFT 长度 = FIXED_FFT_LEN
     *
     * 关键修复：无论输入信号多长，都 pad 到 FIXED_FFT_LEN 做 FFT，
     * 再用相同尺寸逆变换回来。训练/测试特征不再因信号长度不同而偏移。
     */
    public static double[] bandpassFilter(double[] signal, double lowFreq, double highFreq) {
        int n = signal.length;
        int fftLen = FIXED_FFT_LEN;

        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

        // 零均值
        double mean = 0;
        for (double v : signal) mean += v;
        mean /= n;

        // pad 到固定长度
        double[] padded = new double[fftLen];
        for (int i = 0; i < n; i++) padded[i] = signal[i] - mean;

        // FFT
        Complex[] spectrum = fft.transform(padded, TransformType.FORWARD);

        // 高斯窗带通滤波
        double centerFreq = (lowFreq + highFreq) / 2.0;
        double halfWidth = (highFreq - lowFreq) / 2.0;
        double sigma = halfWidth / 2.0;

        for (int i = 0; i <= fftLen / 2; i++) {
            double freq = (double) i * SAMPLE_RATE / fftLen;
            double gain = gaussianBandpassGain(freq, centerFreq, halfWidth, sigma);
            spectrum[i] = spectrum[i].multiply(gain);
            if (i > 0) {
                spectrum[fftLen - i] = spectrum[fftLen - i].multiply(gain);
            }
        }

        // IFFT → 取回原长度
        Complex[] timeDomain = fft.transform(spectrum, TransformType.INVERSE);
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = timeDomain[i].getReal();
        }

        return result;
    }

    /**
     * 对 Trial 所有通道做带通滤波
     */
    public static double[][] bandpassFilterTrial(Trial trial, double lowFreq, double highFreq) {
        int channels = trial.data.length;
        int samples = trial.data[0].length;
        double[][] result = new double[channels][samples];
        for (int ch = 0; ch < channels; ch++) {
            result[ch] = bandpassFilter(trial.data[ch], lowFreq, highFreq);
        }
        return result;
    }

    private static double gaussianBandpassGain(double freq, double center, double halfWidth, double sigma) {
        double dist = Math.abs(freq - center);
        if (dist <= halfWidth) return 1.0;
        return Math.exp(-0.5 * Math.pow((dist - halfWidth) / sigma, 2));
    }
}
