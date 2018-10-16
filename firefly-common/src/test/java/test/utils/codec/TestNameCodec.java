package test.utils.codec;

import com.firefly.utils.codec.NameCodec;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

/**
 * @author Pengtao Qiu
 */
public class TestNameCodec {

    @Test
    public void test() {
        List<String> numList = IntStream.range(1, 10000)
                                        .boxed()
                                        .map(Object::toString)
                                        .collect(Collectors.toList());
        long orignLength = 0;
        long encodedLength = 0;
        for (String s : numList) {
            orignLength += s.length();
        }

        TreeSet<String> strSet = new TreeSet<>(numList);
        System.out.println(strSet);
        TreeSet<String> encodedStrSet = new TreeSet<>();
        numList.forEach(n -> encodedStrSet.add(NameCodec.encode(n)));
        for (String s : encodedStrSet) {
            encodedLength += s.length();
        }

        List<String> decodedList = encodedStrSet.stream()
                                                .map(NameCodec::decode)
                                                .collect(Collectors.toList());
        Assert.assertThat(decodedList, is(numList));

        double expansionRate = (double) encodedLength / (double) orignLength;
        System.out.println(expansionRate);
        Assert.assertThat(expansionRate, lessThan(2.00));
    }
}
