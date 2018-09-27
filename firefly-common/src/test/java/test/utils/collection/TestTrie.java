package test.utils.collection;

import com.firefly.utils.collection.ArrayTrie;
import com.firefly.utils.collection.TreeTrie;
import com.firefly.utils.collection.Trie;

public class TestTrie {

    public static void main(String[] args) {
        Trie<String> trie2 = new TreeTrie<String>();
        trie2.put("com.firefly.foo.bar");
        trie2.put("com.firefly.foo");

        System.out.println(trie2.keySet());
        System.out.println(trie2.getBest("com.firefly.foo.Test"));
        System.out.println(trie2.getBest("com.firefly.foo.bar.Hello"));
    }

    public static void main2(String[] args) {
        Trie<Integer> trie = new ArrayTrie<>(128);
        for (int i = 0; i < 100; i++) {
            trie.put("hello", 1);
            trie.put("He", 2);
            trie.put("HELL", 3);
            trie.put("wibble", 4);
            trie.put("Wobble", 5);
            trie.put("foo-bar", 6);
            trie.put("foo+bar", 7);
            trie.put("HELL4", 8);
            trie.put("", 9);

            System.out.println(trie.isFull());
        }
        trie.put("prefix1", 1);
        System.out.println(trie.isFull());
        System.out.println(trie.getBest("wib"));


//        for (int i = 0; i < 116; i++) {
//        	boolean r = trie.put("prefix" + i, i);
//        	if(!r)
//        		break;
//		}
//
//        System.out.println(trie.toString());
//        System.out.println(trie.isFull());
    }

}
