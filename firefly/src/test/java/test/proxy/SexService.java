package test.proxy;

import com.firefly.annotation.Component;

/**
 * @author Pengtao Qiu
 */
@Component
public class SexService {

    public String getSex() {
        return "female";
    }
}
