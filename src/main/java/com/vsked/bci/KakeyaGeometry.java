package com.vsked.bci;

import org.apache.commons.math3.linear.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 三维挂谷猜想驱动的 SPD 流形特征压缩
 *
 * 数学对应:
 * ─────────────────────────────────────────────────────────────
 * 挂谷猜想 (R^3)          →  BCI SPD 流形
 * ─────────────────────────────────────────────────────────────
 * 每个方向上的单位线段       →  切空间中 logm 矩阵的特征向量 (空间模式方向)
 * 最小覆盖集的 Hausdorff 维数  →  判别信息仅需 K << n 个主特征方向
 * 针的重叠区域              →  特征值衰减: λ_k 越大, 该方向的"能量"越集中
 * 挂谷维数 = 3             →  EEG 判别的有效秩 ≤ 8 (运动想象在 mu/beta 频带稀疏)
 * ─────────────────────────────────────────────────────────────
 *
 * 核心公式:
 *   对频段 b 的 trial:
 *     1. C → SPD(n) 协方差矩阵
 *     2. G_b = RiemannianMean({C_i})        ← 该频段的流形中心
 *     3. L = logm( G_b^(-1/2) · C · G_b^(-1/2) )  ← 切空间对称矩阵
 *     4. 特征分解: L = V Λ V^T
 *     5. 挂谷特征 = [λ_1,...,λ_K]          ← K 个针的半径 (最大特征值)
 *                 + [L_11,...,L_nn]         ← 每个通道的切空间位移
 *                 + ||L||_F                 ← 总体位移量 (Frobenius 范数)
 *     → K + n + 1 维  (vs 完整切空间 n(n+1)/2 = 253 维)
 *
 * 参考:
 *   - T. Tao, "The Kakeya conjecture in R^3" (证明方向, 2000)
 *   - Barachant et al., "Multiclass BCI by Riemannian Geometry" (2012)
 */
public class KakeyaGeometry {

    /** 全局挂谷方向数 K: 保留特征值最大的前 K 个方向 */
    public static final int KAKYEA_K = 6;

    /** 避免除零 */
    private static final double EPS = 1e-12;

    // ==================== 挂谷切空间特征 ====================

    /**
     * 挂谷压缩：将协方差矩阵 C 投影到参考点 G 的切空间，只保留 K 个主特征方向。
     *
     * @param C  trial 的 SPD 协方差矩阵
     * @param G  该频段的黎曼参考点
     * @param K  保留的主特征方向数 (挂谷维数上界)
     * @return   挂谷特征向量 [λ_1..λ_K, L_diag(0..n-1), frob]
     */
    public static double[] kakeyaTangentFeatures(RealMatrix C, RealMatrix G, int K) {
        int n = C.getRowDimension();
        K = Math.min(K, n);

        // 1. 切空间投影: L = logm(G^{-1/2} · C · G^{-1/2})
        RealMatrix Gis = RiemannianGeometry.invSqrtm(G);
        RealMatrix L = RiemannianGeometry.logm(Gis.multiply(C).multiply(Gis));

        // 2. 特征分解 → 排序 (按绝对值降序, 保留符号)
        EigenDecomposition eig = new EigenDecomposition(L);
        double[] ev = eig.getRealEigenvalues();

        // 按绝对值降序索引
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Double.compare(Math.abs(ev[b]), Math.abs(ev[a])));

        // 3. 构造挂谷特征
        int dim = K + n + 1;
        double[] feats = new double[dim];
        int idx = 0;

        // 3a. 前 K 个特征值 (挂谷针半径, 保留符号)
        for (int k = 0; k < K; k++) {
            feats[idx++] = ev[order[k]];
        }

        // 3b. 对角元素 (每个通道在切空间中的自位移)
        for (int i = 0; i < n; i++) {
            feats[idx++] = L.getEntry(i, i);
        }

        // 3c. Frobenius 范数 (总体切空间距离)
        feats[idx++] = L.getFrobeniusNorm();

        return feats;
    }

    /**
     * 快捷版本：使用默认 K
     */
    public static double[] kakeyaTangentFeatures(RealMatrix C, RealMatrix G) {
        return kakeyaTangentFeatures(C, G, KAKYEA_K);
    }

    // ==================== 挂谷方向重叠分析 (诊断用) ====================

    /**
     * 计算两个类别的挂谷方向重叠度
     * 重叠度越低 → 两类在切空间中越可分 → CSP 先验越强
     *
     * @param covsA  A 类的协方差矩阵列表
     * @param covsB  B 类的协方差矩阵列表
     * @param G      该频段的黎曼均值
     * @param K      保留的主方向数
     * @return       重叠度: 0 (完全正交) ~ 1 (完全相同)
     */
    public static double kakeyaOverlap(List<RealMatrix> covsA, List<RealMatrix> covsB,
                                       RealMatrix G, int K) {
        int n = covsA.get(0).getRowDimension();
        K = Math.min(K, n);
        RealMatrix Gis = RiemannianGeometry.invSqrtm(G);

        // A 类的平均切空间
        RealMatrix LA = MatrixUtils.createRealMatrix(n, n);
        for (RealMatrix C : covsA) {
            LA = LA.add(RiemannianGeometry.logm(Gis.multiply(C).multiply(Gis)));
        }
        LA = LA.scalarMultiply(1.0 / covsA.size());

        // B 类的平均切空间
        RealMatrix LB = MatrixUtils.createRealMatrix(n, n);
        for (RealMatrix C : covsB) {
            LB = LB.add(RiemannianGeometry.logm(Gis.multiply(C).multiply(Gis)));
        }
        LB = LB.scalarMultiply(1.0 / covsB.size());

        // 各自的 top-K 特征向量
        RealVector[] eigA = topKEigenvectors(LA, K);
        RealVector[] eigB = topKEigenvectors(LB, K);

        // 方向重叠 = K 对 K 的内积绝对值的平均值
        double overlap = 0;
        for (int i = 0; i < K; i++) {
            for (int j = 0; j < K; j++) {
                overlap += Math.abs(eigA[i].dotProduct(eigB[j]));
            }
        }
        return overlap / (K * K);
    }

    // ==================== 挂谷特征维度名称 (调试) ====================

    public static String[] featureNames(int n, int K) {
        K = Math.min(K, n);
        String[] names = new String[K + n + 1];
        int idx = 0;
        for (int k = 0; k < K; k++)      names[idx++] = "λ_" + (k + 1);
        for (int i = 0; i < n; i++)      names[idx++] = "diag_" + i;
        names[idx++] = "frob";
        return names;
    }

    // ==================== 内部工具 ====================

    /**
     * 返回按特征值绝对值降序排列的前 K 个特征向量 (归一化的方向)
     */
    private static RealVector[] topKEigenvectors(RealMatrix L, int K) {
        int n = L.getRowDimension();
        EigenDecomposition eig = new EigenDecomposition(L);
        double[] ev = eig.getRealEigenvalues();
        RealMatrix V = eig.getV();

        // 按特征值绝对值排序
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble(a -> -Math.abs(ev[a])));

        RealVector[] vecs = new RealVector[K];
        for (int k = 0; k < K; k++) {
            vecs[k] = V.getColumnVector(order[k]);
        }
        return vecs;
    }
}
