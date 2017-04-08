package test.utils.classproxy;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.classproxy.*;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class AbstractProxyFactoryTest {

    @Parameterized.Parameter
    public Run r;

    static class Run {
        ReflectUtils.ProxyFactory proxyFactory;
        String name;

        @Override
        public String toString() {
            return name;
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Run> data() {
        List<Run> list = new ArrayList<>();
        Run run = new Run();
        run.proxyFactory = JavaReflectionProxyFactory.INSTANCE;
        run.name = "java proxy factory";
        list.add(run);

        run = new Run();
        run.proxyFactory = JavassistReflectionProxyFactory.INSTANCE;
        run.name = "javassist proxy factory";
        list.add(run);
        return list;
    }

    public static class Foo {
        private boolean failure;
        public String name;
        private int number;
        private double price;

        public int num2;
        public String info;

        private String iPhone;
        private boolean iPad;

        public String getiPhone() {
            return iPhone;
        }

        public void setiPhone(String iPhone) {
            this.iPhone = iPhone;
        }

        public boolean isiPad() {
            return iPad;
        }

        public void setiPad(boolean iPad) {
            this.iPad = iPad;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public boolean isFailure() {
            return failure;
        }

        public void setFailure(boolean failure) {
            this.failure = failure;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setProperty(String name, boolean failure) {
            this.name = name;
            this.failure = failure;
        }

    }
}
