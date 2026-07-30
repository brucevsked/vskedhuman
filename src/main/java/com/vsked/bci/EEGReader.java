package com.vsked.bci;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class EEGReader {

    // 配置参数
    static final int SAMPLE_RATE = 250;       // 数据集采样率通常是 250Hz
    static final int TRIAL_DURATION_SEC = 3;  // 我们只取提示后的 3 秒数据
    static final int TRIAL_SAMPLES = SAMPLE_RATE * TRIAL_DURATION_SEC; // 750个点

    public static void main(String[] args) {
        String filePath = "D:/bci/BCICIV_2a_gdf/csv/A01T.csv";

        List<Trial> trialList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            // 1. 读取表头，确定总共有多少列（多少个通道）
            String header = br.readLine();
            String[] headers = header.split(",");
            int numCols = headers.length;
            int numChannels = numCols - 1; // 减去 stimulus 列

            System.out.println("总列数: " + numCols);
            System.out.println("EEG 通道数: " + numChannels);

            // 临时缓冲区，用来存每个通道的数据
            List<List<Double>> buffer = new ArrayList<>();
            for(int i=0; i<numChannels; i++) buffer.add(new ArrayList<>());

            int currentLabel = 0;
            int samplesInCurrentTrial = 0;
            int trialCount = 0;

            // 2. 逐行扫描
            while ((line = br.readLine()) != null) {
                String[] vals = line.split(",");

                // 获取 stimulus 列（最后一列）
                double eventVal = Double.parseDouble(vals[vals.length - 1]);
                int eventCode = (int) eventVal;

                // 只关注 769, 770, 771, 772（4个类别）
                boolean isTaskStart = (eventCode == 769 || eventCode == 770 ||
                        eventCode == 771 || eventCode == 772);

                // 状态机逻辑：检测到新任务开始
                if (isTaskStart && currentLabel == 0) {
                    currentLabel = eventCode;
                    samplesInCurrentTrial = 0;
                    trialCount++;
                    System.out.println("开始记录 Trial #" + trialCount +
                            ", 标签: " + currentLabel);
                    // 清空缓冲区
                    for(List<Double> list : buffer) list.clear();
                }

                // 如果正在任务中，记录数据
                if (currentLabel != 0) {
                    // 把每个通道的数据塞进缓冲区
                    // 跳过 stimulus 列，只取前 numChannels 列
                    for (int ch = 0; ch < numChannels; ch++) {
                        buffer.get(ch).add(Double.parseDouble(vals[ch]));
                    }
                    samplesInCurrentTrial++;

                    // 采够了 3 秒（750个点），就存盘
                    if (samplesInCurrentTrial >= TRIAL_SAMPLES) {
                        Trial t = new Trial(numChannels, TRIAL_SAMPLES);
                        t.label = currentLabel;

                        // 从缓冲区拷贝到数组
                        for (int ch = 0; ch < numChannels; ch++) {
                            for (int s = 0; s < TRIAL_SAMPLES; s++) {
                                t.data[ch][s] = buffer.get(ch).get(s);
                            }
                        }
                        trialList.add(t);

                        // 重置状态，等待下一个任务
                        currentLabel = 0;
                        samplesInCurrentTrial = 0;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n切分完成！共获得 " + trialList.size() + " 个有效动作片段");

        // 简单统计一下标签
        int count1=0, count2=0, count3=0, count4=0;
        for(Trial t : trialList) {
            if(t.label==769) count1++;
            if(t.label==770) count2++;
            if(t.label==771) count3++;
            if(t.label==772) count4++;
        }
        System.out.println("分布统计:");
        System.out.println("  左手(769):  " + count1 + " 个");
        System.out.println("  右手(770):  " + count2 + " 个");
        System.out.println("  脚(771):    " + count3 + " 个");
        System.out.println("  舌(772):    " + count4 + " 个");
    }
}



