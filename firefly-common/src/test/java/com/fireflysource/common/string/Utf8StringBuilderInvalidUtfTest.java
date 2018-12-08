package com.fireflysource.common.string;


import com.fireflysource.common.object.TypeUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test various invalid UTF8 byte sequences.
 */
class Utf8StringBuilderInvalidUtfTest {

    @ParameterizedTest
    @ValueSource(strings = {"c0af", "EDA080", "f08080af", "f8808080af", "e080af", "F4908080", "fbbfbfbfbf", "10FFFF",
            "CeBaE1BdB9Cf83CeBcCeB5EdA080656469746564", "da07", "d807", "EDA087"})
    void testInvalidUTF8(String hex) {
        byte[] bytes = TypeUtils.fromHexString(hex);
        System.out.printf("Utf8StringBuilderInvalidUtfTest (%s)%n", TypeUtils.toHexString(bytes));

        assertThrows(Utf8Appendable.NotUtf8Exception.class, () -> {
            Utf8StringBuilder buffer = new Utf8StringBuilder();
            buffer.append(bytes, 0, bytes.length);
        });
    }
}
