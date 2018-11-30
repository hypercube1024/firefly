package com.firefly.kotlin.ext.common

/**
 * @author Pengtao Qiu
 */
fun String.levenshteinDistance(s2: String): Double {
    val d: Array<DoubleArray>// matrix
    val n: Int = this.length
    val m: Int = s2.length
    var i = 0
    var j: Int
    var si: Char
    var tj: Char

    var cost: Double// cost
    // Step1
    if (n == 0) {
        return m.toDouble()
    }
    if (m == 0) {
        return n.toDouble()
    }
    d = Array(n + 1) { DoubleArray(m + 1) }
    // Step2
    while (i <= n) {
        d[i][0] = i.toDouble()
        i++
    }
    j = 0
    while (j <= m) {
        d[0][j] = j.toDouble()
        j++
    }
    // Step3
    i = 1
    while (i <= n) {
        si = this[i - 1]
        // Step4
        j = 1
        while (j <= m) {
            tj = s2[j - 1]
            // Step5
            cost = if (si == tj) {
                0.0
            } else {
                1.0
            }
            // Step6
            d[i][j] = minimum(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
            j++
        }
        i++
    }
    // Step7
    return d[n][m]
}

private fun minimum(a: Double, b: Double, c: Double): Double {
    var mi: Double
    mi = a
    if (b < mi) {
        mi = b
    }
    if (c < mi) {
        mi = c
    }
    return mi
}