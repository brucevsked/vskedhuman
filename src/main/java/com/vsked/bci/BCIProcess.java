package com.vsked.bci;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BCIProcess {

    static final int SAMPLE_RATE = 250;
    static final int TRIAL_DURATION_SEC = 3;
    static final int TRIAL_SAMPLES = SAMPLE_RATE * TRIAL_DURATION_SEC;

    public static void main(String[] args) {
        String filePath = args.length > 0 ? args[0] : "D:/bci/BCICIV_2a_gdf/csv/A01T.csv";
        List<Trial> allTrials = readData(filePath);

        if (allTrials.isEmpty()) {
            System.out.println("没有读取到数据！请检查文件路径: " + filePath);
            return;
        }

        List<Trial> binaryTrials = allTrials.stream()
            .filter(t -> t.label == 769 || t.label == 770)
            .collect(Collectors.toList());

        System.out.println("\n总 trial 数: " + allTrials.size());
        System.out.println("左手(769) + 右手(770): " + binaryTrials.size());
        long nLeft = binaryTrials.stream().filter(t -> t.label == 769).count();
        long nRight = binaryTrials.stream().filter(t -> t.label == 770).count();
        System.out.printf("  左手: %d,  右手: %d%n", nLeft, nRight);

        Collections.shuffle(binaryTrials, new java.util.Random(42));
        int splitIndex = (int) (binaryTrials.size() * 0.8);
        List<Trial> trainSet = new ArrayList<>(binaryTrials.subList(0, splitIndex));
        List<Trial> testSet = new ArrayList<>(binaryTrials.subList(splitIndex, binaryTrials.size()));

        System.out.printf("%n数据集划分: 训练=%d, 测试=%d%n", trainSet.size(), testSet.size());

        BCIPipeline pipeline = new BCIPipeline();
        pipeline.train(trainSet);

        System.out.println("\n========== 测试结果 (左手 769 vs 右手 770) ==========");
        int correct = 0, total = 0;
        int correctLeft = 0, totalLeft = 0;
        int correctRight = 0, totalRight = 0;

        for (Trial t : testSet) {
            int predicted = pipeline.predict(t);
            if (predicted == t.label) correct++;
            if (t.label == 769) {
                totalLeft++;
                if (predicted == 769) correctLeft++;
            } else {
                totalRight++;
                if (predicted == 770) correctRight++;
            }
            total++;
        }

        System.out.printf("总体准确率: %.2f%%  (%d / %d)%n", 100.0 * correct / total, correct, total);
        System.out.printf("左手(769): %.2f%%  (%d / %d)%n",
            totalLeft > 0 ? 100.0 * correctLeft / totalLeft : 0, correctLeft, totalLeft);
        System.out.printf("右手(770): %.2f%%  (%d / %d)%n",
            totalRight > 0 ? 100.0 * correctRight / totalRight : 0, correctRight, totalRight);
    }

    static List<Trial> readData(String filePath) {
        System.out.println("正在读取数据: " + filePath);
        List<Trial> trialList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                System.err.println("文件为空！");
                return trialList;
            }

            String[] headers = headerLine.split(",");
            int numCols = headers.length;

            // 自动识别 EEG 通道列（列名以 "EEG-" 开头）
            List<Integer> eegColIndices = new ArrayList<>();
            int stimColIndex = -1;
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim();
                if (h.startsWith("EEG-")) {
                    eegColIndices.add(i);
                } else if (h.equalsIgnoreCase("stimulus") || h.equalsIgnoreCase("label")) {
                    stimColIndex = i;
                }
            }

            int numEEG = eegColIndices.size();
            if (numEEG == 0) {
                System.err.println("未检测到 EEG 通道！请检查文件格式。");
                System.err.println("列名: " + headerLine);
                return trialList;
            }
            if (stimColIndex == -1) {
                stimColIndex = numCols - 1;
            }

            System.out.printf("  CSV 列数: %d, EEG 通道: %d, 标签列: %d%n",
                numCols, numEEG, stimColIndex);
            System.out.println("  EEG 通道: " + eegColIndices.stream()
                .map(i -> headers[i]).collect(Collectors.joining(", ")));

            // 读取数据
            List<List<Double>> buffer = new ArrayList<>();
            for (int i = 0; i < numEEG; i++) buffer.add(new ArrayList<>());

            int currentLabel = 0;
            int samplesInCurrentTrial = 0;
            int trialCount = 0;
            String line;

            while ((line = br.readLine()) != null) {
                String[] vals = line.split(",");
                if (vals.length < numCols) continue;

                int eventCode = (int) Double.parseDouble(vals[stimColIndex].trim());

                boolean isTaskStart = (eventCode == 769 || eventCode == 770
                    || eventCode == 771 || eventCode == 772);

                if (isTaskStart && currentLabel == 0) {
                    currentLabel = eventCode;
                    samplesInCurrentTrial = 0;
                    trialCount++;
                    for (List<Double> list : buffer) list.clear();
                }

                if (currentLabel != 0) {
                    for (int ci = 0; ci < numEEG; ci++) {
                        buffer.get(ci).add(Double.parseDouble(vals[eegColIndices.get(ci)]));
                    }
                    samplesInCurrentTrial++;

                    if (samplesInCurrentTrial >= TRIAL_SAMPLES) {
                        Trial t = new Trial(numEEG, TRIAL_SAMPLES);
                        t.label = currentLabel;
                        for (int ch = 0; ch < numEEG; ch++) {
                            for (int s = 0; s < TRIAL_SAMPLES; s++) {
                                t.data[ch][s] = buffer.get(ch).get(s);
                            }
                        }
                        trialList.add(t);
                        currentLabel = 0;
                        samplesInCurrentTrial = 0;
                    }
                }
            }
            System.out.println("  共读取 " + trialCount + " 个 trial (有效: " + trialList.size() + ")");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return trialList;
    }
}
