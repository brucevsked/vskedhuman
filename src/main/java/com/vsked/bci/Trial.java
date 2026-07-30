package com.vsked.bci;

/**
 * 这是一个“动作”：比如“想象左手”持续了3秒钟
 */
public class Trial {
    public double[][] data; // data[通道][时间点]
    public int label;       // 标签：比如 1=左手, 2=右手

    public Trial(int channels, int samples) {
        this.data = new double[channels][samples];
    }
}

