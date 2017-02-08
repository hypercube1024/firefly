package com.firefly.utils.math;

/**
 * @author Pengtao Qiu
 */
abstract public class MatrixUtils {

    public double[][] mul(double[][] a, double[][] b) {
        int m = a.length, n = a[0].length, p = b[0].length;
        double[][] x = new double[m][p];
        double[][] c = new double[p][n];
        for (int i = 0; i < n; ++i) // transpose
            for (int j = 0; j < p; ++j)
                c[j][i] = b[i][j];
        for (int i = 0; i < m; ++i)
            for (int j = 0; j < p; ++j) {
                double s = 0.0;
                for (int k = 0; k < n; ++k)
                    s += a[i][k] * c[j][k];
                x[i][j] = s;
            }
        return x;
    }

    public long[][] mul(long[][] a, long[][] b) {
        int m = a.length, n = a[0].length, p = b[0].length;
        long[][] x = new long[m][p];
        long[][] c = new long[p][n];
        for (int i = 0; i < n; ++i) // transpose
            for (int j = 0; j < p; ++j)
                c[j][i] = b[i][j];
        for (int i = 0; i < m; ++i)
            for (int j = 0; j < p; ++j) {
                long s = 0;
                for (int k = 0; k < n; ++k)
                    s += a[i][k] * c[j][k];
                x[i][j] = s;
            }
        return x;
    }

    public int[][] mul(int[][] a, int[][] b) {
        int m = a.length, n = a[0].length, p = b[0].length;
        int[][] x = new int[m][p];
        int[][] c = new int[p][n];
        for (int i = 0; i < n; ++i) // transpose
            for (int j = 0; j < p; ++j)
                c[j][i] = b[i][j];
        for (int i = 0; i < m; ++i)
            for (int j = 0; j < p; ++j) {
                int s = 0;
                for (int k = 0; k < n; ++k)
                    s += a[i][k] * c[j][k];
                x[i][j] = s;
            }
        return x;
    }
}
