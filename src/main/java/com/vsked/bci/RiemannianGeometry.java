package com.vsked.bci;

import org.apache.commons.math3.linear.*;
import java.util.List;

/**
 * 黎曼几何核心运算：矩阵 log/exp、黎曼均值、切空间投影
 *
 * 参考: Barachant et al. "Multiclass BCI Classification by Riemannian Geometry" (2012)
 */
public class RiemannianGeometry {

    private static final double REG_EPS = 1e-6;  // 协方差正则化系数
    private static final int    MEAN_MAX_ITER = 50;
    private static final double MEAN_TOL = 1e-6;

    // ==================== 协方差矩阵 ====================

    /**
     * 计算 Trial 的空间协方差矩阵 (channels × channels)
     * C = (1/(T-1)) * X_centered * X_centered^T + ε·tr(C)/n·I
     */
    public static RealMatrix computeCovariance(Trial trial) {
        return computeCovariance(trial.data);
    }

    /**
     * 从原始 double[][] 计算协方差矩阵（用于预滤波后的频段数据）
     */
    public static RealMatrix computeCovariance(double[][] data) {
        int channels = data.length;
        int samples = data[0].length;

        RealMatrix X = MatrixUtils.createRealMatrix(channels, samples);
        for (int ch = 0; ch < channels; ch++) {
            double m = 0;
            for (int t = 0; t < samples; t++) m += data[ch][t];
            m /= samples;
            for (int t = 0; t < samples; t++) X.setEntry(ch, t, data[ch][t] - m);
        }

        RealMatrix C = X.multiply(X.transpose()).scalarMultiply(1.0 / (samples - 1));

        // 正则化确保正定
        double trace = 0;
        for (int i = 0; i < channels; i++) trace += C.getEntry(i, i);
        double alpha = REG_EPS * trace / channels;
        RealMatrix I = MatrixUtils.createRealIdentityMatrix(channels);
        return C.add(I.scalarMultiply(alpha));
    }

    // ==================== 矩阵对数 logm ====================
    // logm(M) = V * log(D) * V^T   (M symmetric positive definite)

    public static RealMatrix logm(RealMatrix M) {
        EigenDecomposition eig = new EigenDecomposition(M);
        RealMatrix V = eig.getV();
        double[] ev = eig.getRealEigenvalues();
        int n = M.getRowDimension();

        double[] logEv = new double[n];
        for (int i = 0; i < n; i++) {
            logEv[i] = Math.log(Math.max(ev[i], 1e-15));
        }
        RealMatrix logD = MatrixUtils.createRealDiagonalMatrix(logEv);
        return V.multiply(logD).multiply(V.transpose());
    }

    // ==================== 矩阵指数 expm ====================
    // expm(M) = V * exp(D) * V^T

    public static RealMatrix expm(RealMatrix M) {
        EigenDecomposition eig = new EigenDecomposition(M);
        RealMatrix V = eig.getV();
        double[] ev = eig.getRealEigenvalues();
        int n = M.getRowDimension();

        double[] expEv = new double[n];
        for (int i = 0; i < n; i++) {
            expEv[i] = Math.exp(ev[i]);
        }
        RealMatrix expD = MatrixUtils.createRealDiagonalMatrix(expEv);
        return V.multiply(expD).multiply(V.transpose());
    }

    // ==================== 矩阵平方根/逆平方根 ====================
    // M^(1/2)  = V * sqrt(D)    * V^T
    // M^(-1/2) = V * 1/sqrt(D)  * V^T

    public static RealMatrix sqrtm(RealMatrix M) {
        EigenDecomposition eig = new EigenDecomposition(M);
        RealMatrix V = eig.getV();
        double[] ev = eig.getRealEigenvalues();
        int n = M.getRowDimension();

        double[] sqrtEv = new double[n];
        for (int i = 0; i < n; i++) {
            sqrtEv[i] = Math.sqrt(Math.max(ev[i], 1e-15));
        }
        RealMatrix sqrtD = MatrixUtils.createRealDiagonalMatrix(sqrtEv);
        return V.multiply(sqrtD).multiply(V.transpose());
    }

    public static RealMatrix invSqrtm(RealMatrix M) {
        EigenDecomposition eig = new EigenDecomposition(M);
        RealMatrix V = eig.getV();
        double[] ev = eig.getRealEigenvalues();
        int n = M.getRowDimension();

        double[] isqrtEv = new double[n];
        for (int i = 0; i < n; i++) {
            isqrtEv[i] = 1.0 / Math.sqrt(Math.max(ev[i], 1e-15));
        }
        RealMatrix isqrtD = MatrixUtils.createRealDiagonalMatrix(isqrtEv);
        return V.multiply(isqrtD).multiply(V.transpose());
    }

    // ==================== 黎曼均值 ====================
    // 迭代算法：G_{k+1} = G_k^(1/2) * expm( mean(logm(G_k^(-1/2) * C_i * G_k^(-1/2))) ) * G_k^(1/2)

    public static RealMatrix riemannianMean(List<RealMatrix> matrices) {
        if (matrices.isEmpty()) throw new IllegalArgumentException("空矩阵列表");
        int n = matrices.get(0).getRowDimension();

        // 初始化：算术平均
        RealMatrix G = MatrixUtils.createRealMatrix(n, n);
        for (RealMatrix m : matrices) G = G.add(m);
        G = G.scalarMultiply(1.0 / matrices.size());

        for (int iter = 0; iter < MEAN_MAX_ITER; iter++) {
            RealMatrix Gis = invSqrtm(G);

            // 计算各点在 G 处切空间中的均值
            RealMatrix meanTangent = MatrixUtils.createRealMatrix(n, n);
            for (RealMatrix C : matrices) {
                RealMatrix M = Gis.multiply(C).multiply(Gis);
                meanTangent = meanTangent.add(logm(M));
            }
            meanTangent = meanTangent.scalarMultiply(1.0 / matrices.size());

            // 收敛判断
            double frob = meanTangent.getFrobeniusNorm();
            if (frob < MEAN_TOL) break;

            // 更新
            RealMatrix Gs = sqrtm(G);
            G = Gs.multiply(expm(meanTangent)).multiply(Gs);
        }
        return G;
    }

    // ==================== 切空间投影 ====================

    /**
     * 将协方差矩阵投影到参考点 G 的切空间
     * tangent_i = upper( logm( G^(-1/2) * C_i * G^(-1/2) ) )
     *
     * 返回特征向量：对角元素 + sqrt(2)*上三角非对角元素
     * 22ch → 22*23/2 = 253 维
     */
    public static double[] tangentSpaceProjection(RealMatrix C, RealMatrix G) {
        int n = C.getRowDimension();
        RealMatrix Gis = invSqrtm(G);
        RealMatrix M = Gis.multiply(C).multiply(Gis);
        RealMatrix S = logm(M);  // 对称矩阵

        // vectorize: 保留 Frobenius 范数
        int dim = n * (n + 1) / 2;
        double[] vec = new double[dim];
        int idx = 0;
        for (int i = 0; i < n; i++) {
            vec[idx++] = S.getEntry(i, i); // 对角
            for (int j = i + 1; j < n; j++) {
                vec[idx++] = Math.sqrt(2.0) * S.getEntry(i, j); // 上三角×√2
            }
        }
        return vec;
    }

    /**
     * 计算两个 SPD 矩阵之间的黎曼距离
     */
    public static double riemannianDistance(RealMatrix A, RealMatrix B) {
        RealMatrix Ais = invSqrtm(A);
        RealMatrix M = Ais.multiply(B).multiply(Ais);
        RealMatrix logM = logm(M);
        return logM.getFrobeniusNorm();
    }
}
