package com.vsked.bci;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

/**
 * 特征提取器：兼容旧接口
 * 实际管线已迁移到 BCIPipeline + FBCCSPFilter + FeatureSelector
 */
public class FeatureExtractor {

    public static double[] extractFeatures(Trial trial) {
        int channels = trial.data.length;
        int samples = trial.data[0].length;
        double[] features = new double[channels * 2];
        StandardDeviation std = new StandardDeviation();

        for (int ch = 0; ch < channels; ch++) {
            double[] channelData = trial.data[ch];
            double totalEnergy = 0;
            for (double val : channelData) totalEnergy += val * val;
            features[ch * 2] = totalEnergy / samples;
            features[ch * 2 + 1] = std.evaluate(channelData);
        }
        return features;
    }
}
