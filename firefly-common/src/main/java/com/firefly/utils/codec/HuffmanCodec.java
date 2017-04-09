package com.firefly.utils.codec;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class HuffmanCodec<T> implements Serializable {

    private static final long serialVersionUID = -5318250039712365557L;

    abstract public static class HuffmanTree<T> implements Comparable<HuffmanTree<T>>, Serializable {
        private static final long serialVersionUID = -5354103251920897803L;
        public final Long frequency; // the frequency of this tree

        public HuffmanTree(Long frequency) {
            this.frequency = frequency;
        }

        @Override
        public int compareTo(HuffmanTree<T> tree) {
            return frequency.compareTo(tree.frequency);
        }
    }

    public static class HuffmanLeaf<T> extends HuffmanTree<T> {
        private static final long serialVersionUID = -8197406618091612264L;
        public final T value;

        public HuffmanLeaf(Long freq, T value) {
            super(freq);
            this.value = value;
        }

        @Override
        public String toString() {
            return "HuffmanLeaf [value=" + value + ", frequency=" + frequency
                    + "]";
        }

    }

    public static class HuffmanNode<T> extends HuffmanTree<T> {
        private static final long serialVersionUID = -4581114135719242316L;
        public final HuffmanTree<T> left, right; // subtrees

        public HuffmanNode(HuffmanTree<T> left, HuffmanTree<T> right) {
            super(left.frequency + right.frequency);
            this.left = left;
            this.right = right;
        }
    }

    public static class HuffmanCode implements Serializable {

        private static final long serialVersionUID = -4696130695208200688L;
        public final Long frequency;
        public final int length;
        public final String bits;
        public final BitBuilder bitBuilder;

        public HuffmanCode(Long frequency, String bits) {
            super();
            this.frequency = frequency;
            this.length = bits.length();
            this.bits = bits;
            bitBuilder = new BitBuilder(length);
            for (int i = 0; i < bits.length(); i++) {
                bitBuilder.append(bits.charAt(i) == '1');
            }
        }

        @Override
        public String toString() {
            return "HuffmanCode [frequency=" + frequency + ", length=" + length
                    + ", bits=" + bits + ", getBytes()="
                    + Arrays.toString(getBytes()) + "]";
        }

        public byte[] getBytes() {
            return bitBuilder.toByteArray();
        }
    }

    public static class BitBuilder extends BitSet {

        private static final long serialVersionUID = 4678685861273345213L;
        private int length;
        private int index;

        public BitBuilder() {
            super();
        }

        public BitBuilder(int len) {
            super(len);
        }

        public int getLength() {
            return length;
        }

        public BitBuilder append(boolean value) {
            set(index, value);
            index++;
            length++;
            return this;
        }
    }

    private Map<T, HuffmanCode> codecMap;
    private HuffmanTree<T> huffmanTree;

    public HuffmanCodec() {
    }

    public HuffmanCodec(T[] elements) {
        huffmanTree = buildHuffmanTree(elements);
        codecMap = buildHuffmanCodeMap(huffmanTree);
    }

    public Map<T, HuffmanCode> getCodecMap() {
        return codecMap;
    }

    public HuffmanTree<T> getHuffmanTree() {
        return huffmanTree;
    }

    public void setCodecMap(Map<T, HuffmanCode> codecMap) {
        this.codecMap = codecMap;
    }

    public void setHuffmanTree(HuffmanTree<T> huffmanTree) {
        this.huffmanTree = huffmanTree;
    }

    public BitBuilder encode(T[] elements) {
        BitBuilder bits = new BitBuilder();
        for (T t : elements) {
            HuffmanCode code = codecMap.get(t);
            for (int j = 0; j < code.length; j++) {
                bits.append(code.bitBuilder.get(j));
            }
        }
        return bits;
    }

    public List<T> decode(BitBuilder bits) {
        List<T> elements = new ArrayList<>();
        HuffmanTree<T> currentNode = huffmanTree;
        for (int i = 0; i < bits.getLength(); i++) {
            if (currentNode instanceof HuffmanNode) {
                HuffmanNode<T> node = (HuffmanNode<T>) currentNode;
                currentNode = bits.get(i) ? node.right : node.left;
            }
            if (currentNode instanceof HuffmanLeaf) {
                HuffmanLeaf<T> leaf = (HuffmanLeaf<T>) currentNode;
                elements.add(leaf.value);
                currentNode = huffmanTree;
            }
        }
        return elements;
    }

    public static <T> Map<T, HuffmanCode> buildHuffmanCodeMap(HuffmanTree<T> tree) {
        Map<T, HuffmanCode> map = new HashMap<>();
        StringBuilder bits = new StringBuilder();
        _buildHuffmanCodeMap(tree, map, bits);
        return map;
    }

    private static <T> void _buildHuffmanCodeMap(HuffmanTree<T> tree, Map<T, HuffmanCode> map, StringBuilder bits) {
        if (tree instanceof HuffmanLeaf) {
            HuffmanLeaf<T> leaf = (HuffmanLeaf<T>) tree;

            HuffmanCode code = new HuffmanCode(leaf.frequency, bits.toString());
            map.put(leaf.value, code);
        } else if (tree instanceof HuffmanNode) {
            HuffmanNode<T> node = (HuffmanNode<T>) tree;

            // traverse left
            bits.append('0');
            _buildHuffmanCodeMap(node.left, map, bits);
            bits.deleteCharAt(bits.length() - 1);

            // traverse right
            bits.append('1');
            _buildHuffmanCodeMap(node.right, map, bits);
            bits.deleteCharAt(bits.length() - 1);
        }
    }

    public static <T> HuffmanTree<T> buildHuffmanTree(T[] elements) {
        Map<T, Long> frequencyMap = new HashMap<>();
        for (T t : elements) {
            frequencyMap.merge(t, 1L, (a, b) -> a + b);
        }

        return buildHuffmanTree(frequencyMap);
    }

    public static <T> HuffmanTree<T> buildHuffmanTree(Map<T, Long> frequencyMap) {
        // initially, we have a forest of leaves
        PriorityQueue<HuffmanTree<T>> trees = new PriorityQueue<>();
        for (Map.Entry<T, Long> entry : frequencyMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                trees.offer(new HuffmanLeaf<>(entry.getValue(), entry.getKey()));
            }
        }

        // loop until there is only one tree left
        while (trees.size() > 1) {
            // two trees with least frequency
            HuffmanTree<T> a = trees.poll();
            HuffmanTree<T> b = trees.poll();

            // put into new node and re-insert into queue
            trees.offer(new HuffmanNode<>(a, b));
        }
        return trees.poll();
    }
}
