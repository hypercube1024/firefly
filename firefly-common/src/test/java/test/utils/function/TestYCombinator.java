package test.utils.function;

import com.firefly.utils.function.Func1;

import static com.firefly.utils.function.YCombinator.Y;


/**
 * @author Pengtao Qiu
 */
public class TestYCombinator {

    public static void main(String[] args) {
        Func1<Integer, Integer> fibonacci = Y(f -> n -> (n <= 2) ? 1 : (f.call(n - 1) + f.call(n - 2)));
        Func1<Integer, Integer> factorial = Y(f -> n -> (n <= 1) ? 1 : (n * f.call(n - 1)));

        System.out.println("fibonacci = " + fibonacci.call(5));
        System.out.println("factorial = " + factorial.call(3));
    }
}
