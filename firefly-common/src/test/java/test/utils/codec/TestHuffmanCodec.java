package test.utils.codec;

import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.codec.HexUtils;
import com.firefly.utils.codec.HuffmanCodec;
import com.firefly.utils.codec.HuffmanCodec.BitBuilder;
import com.firefly.utils.codec.HuffmanCodec.HuffmanCode;

public class TestHuffmanCodec {

    @Test
    public void test() {
        String str = "hello world! 测试一些测试";// "beep boop beer!";
        Character[] chars = new Character[str.length()];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = str.charAt(i);
        }

        HuffmanCodec<Character> codec = new HuffmanCodec<>(chars);
        Map<Character, HuffmanCode> map = codec.getCodecMap();
        System.out.println(map);
        System.out.println("str length: " + str.getBytes(StandardCharsets.UTF_8).length);

        // encode
        BitBuilder bits = codec.encode(chars);
        System.out.println("encode length: " + bits.toByteArray().length + "| bit length: " + bits.getLength());
        System.out.println(HexUtils.bytesToHex(bits.toByteArray()));
        System.out.println(bits);
        System.out.println(BitSet.valueOf(bits.toByteArray()));

        // decode
        StringBuilder sb = new StringBuilder();
        List<Character> c = codec.decode(bits);
        for (char ch : c) {
            sb.append(ch);
        }
        System.out.println(sb);
        Assert.assertThat(sb.toString(), is(str));
    }

//    @Test
    public void test2() {
        String data = "1001111111100111111010011110110011111000101001110110010111110000110010001110101111011100001011111111001000101010010001010111100110100110011100111000011011000111010011010110011111011110101100011110101100100111111000010110111000010001000010101111011100001011101101010010001111100110101110011101000011001110011100010010110110001110101111001001110001011011100001000100001110100011111010110100101011110000110101111111110110111111110010111111001001001000101011110000100100010101001001011111111001111110100011101011110111000001010111100010011010110111010010010110001101101011001001110001011011100001000100000110111011000110000010101111000100110110001111001101101000100010101001000011010111101000101011110000110001001111011000100111011001001110001011011100001000100000100011110011110011010010001010100100010010011111010010001110001100111001110001111100011110010111101100100111001000110011001110101111010001010110111110110001001000011110100011101011110100100101000110001111101010101101010101011100100111001000110011001110101111010001101100011101001101011110111000001111010100011101011110100011011000010001000101110010011100100011001100111010111101001111110011111000110100111010111000111000111110111111100111000101001010010000110010000111101001011110001110101111010010011011110010010010111111100111010111101000100011101100101111100011101011100011100011111011110000111101001100010100111010111101001001010000110110101111111001110101111010010111111011000101000111010111000111000011011000111110101101010100111010111101000101010011011101111110001101110000101111111100111010111101000101101101111101010010010001110101110001110000110011111001100011000100100101001110101111010011000110110000100110011001111011111100011001111011010110010011100111110000100110110010011001101001100111001110000100011111111101101001000101010010001011111101010111101100000111011110001110101111010010100111011001011111000011011000111010011010111111111001111110111101100000111010110111010100101001110110010111110001000101010010001000010001000011101011110100101001110110010111110010100111011110001001101111011001111011100100011101011110100100010100101011011101110000001110101000110101111010111011011100001110101100110111101100110111101001100110010";
        BitBuilder bits = new BitBuilder();
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            bits.append(c == '1');
        }
        System.out.println(bits.length());

        Map<Character, Long> frequencyMap = new HashMap<Character, Long>(){{
            put(' ', 100L);
            put(',', 7L);
            put('.', 10L);
            put('a', 36L);
            put('b', 3L);
            put('c', 8L);
            put('d', 18L);
            put('e', 42L);
            put('f', 6L);
            put('g', 13L);
            put('h', 40L);
            put('i', 31L);
            put('j', 1L);
            put('k', 3L);
            put('l', 25L);
            put('m', 11L);
            put('n', 36L);
            put('o', 23L);
            put('p', 3L);
            put('q', 1L);
            put('r', 20L);
            put('s', 25L);
            put('t', 45L);
            put('u', 3L);
            put('v', 1L);
            put('w', 14L);
            put('x', 1L);
            put('y', 5L);
            put('z', 1L);
        }};
        HuffmanCodec.HuffmanTree<Character> tree = HuffmanCodec.buildHuffmanTree(frequencyMap);
        Map<Character, HuffmanCode> codeMap = HuffmanCodec.buildHuffmanCodeMap(tree);
        System.out.println("tree: " + tree.frequency);
        System.out.println(codeMap);
        HuffmanCodec<Character> codec = new HuffmanCodec<>();
        codec.setHuffmanTree(tree);
        codec.setCodecMap(codeMap);

        StringBuilder ret = new StringBuilder();
        codec.decode(bits).forEach(ret::append);
        System.out.println(ret);
    }
}
