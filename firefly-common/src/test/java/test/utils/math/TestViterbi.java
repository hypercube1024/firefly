package test.utils.math;

import com.firefly.utils.math.Viterbi;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static test.utils.math.TestViterbi.Activity.*;
import static test.utils.math.TestViterbi.Weather.RAINY;
import static test.utils.math.TestViterbi.Weather.SUNNY;

/**
 * @author Pengtao Qiu
 */
public class TestViterbi {

    enum Weather {
        RAINY,
        SUNNY,
    }

    enum Activity {
        WALK,
        SHOP,
        CLEAN,
    }

    @Test
    public void test() {
        int[] states = new int[]{RAINY.ordinal(), SUNNY.ordinal()};
        int[] observations = new int[]{WALK.ordinal(), SHOP.ordinal(), CLEAN.ordinal()};
        double[] startProb = new double[]{0.6, 0.4};
        double[][] transProb = new double[][]{
                {0.7, 0.3},
                {0.4, 0.6},
        };
        double[][] emitProb = new double[][]{
                {0.1, 0.4, 0.5},
                {0.6, 0.3, 0.1},
        };
        int[] result = Viterbi.compute(observations, states, startProb, transProb, emitProb);
        List<Weather> hiddenStates = Arrays.stream(result).mapToObj(r -> Weather.values()[r]).collect(Collectors.toList());
        System.out.println(hiddenStates);
        Assert.assertThat(hiddenStates, is(Arrays.asList(SUNNY, RAINY, RAINY)));
    }
}
