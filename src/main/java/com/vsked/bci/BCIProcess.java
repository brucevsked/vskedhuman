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

        // 四分类：左手 769, 右手 770, 双脚 771, 舌头 772
        List<Trial> multiTrials = allTrials.stream()
            .filter(t -> t.label == 769 || t.label == 770 || t.label == 771 || t.label == 772)
            .collect(Collectors.toList());

        System.out.println("\n总 trial 数: " + allTrials.size());
        System.out.println("有效四分类数据: " + multiTrials.size());
        for (int cls : BCIPipeline.ALL_CLASSES) {
            long cnt = multiTrials.stream().filter(t -> t.label == cls).count();
            System.out.printf("  %-6s(%d): %d 个%n", BCIPipeline.className(cls), cls, cnt);
        }

        Collections.shuffle(multiTrials, new java.util.Random(42));
        int splitIndex = (int) (multiTrials.size() * 0.8);
        List<Trial> trainSet = new ArrayList<>(multiTrials.subList(0, splitIndex));
        List<Trial> testSet = new ArrayList<>(multiTrials.subList(splitIndex, multiTrials.size()));

        System.out.printf("%n数据集划分: 训练=%d, 测试=%d%n", trainSet.size(), testSet.size());

        BCIPipeline pipeline = new BCIPipeline();
        pipeline.train(trainSet);

        // ============ 测试 ============
        System.out.println("\n========== 测试结果 ==========");

        int[] totalPerClass = new int[4];
        int[] correctPerClass = new int[4];
        int[][] confusion = new int[4][4];

        for (Trial t : testSet) {
            int pred = pipeline.predict(t);
            int actIdx = labelToIndex(t.label);
            int predIdx = labelToIndex(pred);
            confusion[actIdx][predIdx]++;
            totalPerClass[actIdx]++;
            if (pred == t.label) correctPerClass[actIdx]++;
        }

        int overallCorrect = 0, overallTotal = 0;
        for (int i = 0; i < 4; i++) {
            overallCorrect += correctPerClass[i];
            overallTotal += totalPerClass[i];
        }
        System.out.printf("总体准确率: %.2f%%  (%d / %d)%n%n",
            100.0 * overallCorrect / overallTotal, overallCorrect, overallTotal);

        System.out.println("各类别准确率:");
        for (int i = 0; i < 4; i++) {
            double acc = totalPerClass[i] > 0 ? 100.0 * correctPerClass[i] / totalPerClass[i] : 0;
            System.out.printf("  %-6s(%d): %.2f%%  (%d / %d)%n",
                BCIPipeline.CLASS_NAMES[i], BCIPipeline.ALL_CLASSES[i],
                acc, correctPerClass[i], totalPerClass[i]);
        }

        // 混淆矩阵
        System.out.println("\n混淆矩阵（行=真实, 列=预测）:");
        System.out.print("            ");
        for (int j = 0; j < 4; j++)
            System.out.printf("%-8s", BCIPipeline.CLASS_NAMES[j]);
        System.out.println();
        for (int i = 0; i < 4; i++) {
            System.out.printf("  %-8s", BCIPipeline.CLASS_NAMES[i]);
            for (int j = 0; j < 4; j++) {
                System.out.printf("%-8d", confusion[i][j]);
            }
            System.out.println();
        }
    }

    private static int labelToIndex(int label) {
        for (int i = 0; i < BCIPipeline.ALL_CLASSES.length; i++)
            if (BCIPipeline.ALL_CLASSES[i] == label) return i;
        return -1;
    }

    static List<Trial> readData(String filePath) {
        System.out.println("正在读取数据: " + filePath);
        List<Trial> trialList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();
            if (headerLine == null) { System.err.println("文件为空！"); return trialList; }

            String[] headers = headerLine.split(",");
            int numCols = headers.length;

            // 自动识别 EEG 通道列（列名以 "EEG-" 开头）
            List<Integer> eegCols = new ArrayList<>();
            int stimCol = -1;
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim();
                if (h.startsWith("EEG-")) eegCols.add(i);
                else if (h.equalsIgnoreCase("stimulus") || h.equalsIgnoreCase("label")) stimCol = i;
            }

            int numEEG = eegCols.size();
            if (numEEG == 0) {
                System.err.println("未检测到 EEG 通道！列名: " + headerLine);
                return trialList;
            }
            if (stimCol == -1) stimCol = numCols - 1;

            System.out.printf("  EEG 通道: %d (%s)%n", numEEG,
                eegCols.stream().map(i -> headers[i]).collect(Collectors.joining(", ")));

            List<List<Double>> buffer = new ArrayList<>();
            for (int i = 0; i < numEEG; i++) buffer.add(new ArrayList<>());

            int currentLabel = 0, samplesInTrial = 0, trialCount = 0;
            String line;

            while ((line = br.readLine()) != null) {
                String[] vals = line.split(",");
                if (vals.length < numCols) continue;

                int event = (int) Double.parseDouble(vals[stimCol].trim());
                boolean isStart = (event == 769 || event == 770 || event == 771 || event == 772);

                if (isStart && currentLabel == 0) {
                    currentLabel = event;
                    samplesInTrial = 0;
                    trialCount++;
                    for (List<Double> buf : buffer) buf.clear();
                }

                if (currentLabel != 0) {
                    for (int ci = 0; ci < numEEG; ci++)
                        buffer.get(ci).add(Double.parseDouble(vals[eegCols.get(ci)]));
                    samplesInTrial++;

                    if (samplesInTrial >= TRIAL_SAMPLES) {
                        Trial t = new Trial(numEEG, TRIAL_SAMPLES);
                        t.label = currentLabel;
                        for (int ch = 0; ch < numEEG; ch++)
                            for (int s = 0; s < TRIAL_SAMPLES; s++)
                                t.data[ch][s] = buffer.get(ch).get(s);
                        trialList.add(t);
                        currentLabel = 0;
                        samplesInTrial = 0;
                    }
                }
            }
            System.out.println("  共读取 " + trialCount + " 个 trial (解析成功: " + trialList.size() + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trialList;
    }
}
