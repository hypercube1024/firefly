package test.proxy;

import com.firefly.annotation.Component;
import com.firefly.annotation.Proxy;

/**
 * @author Pengtao Qiu
 */
@Proxy(proxyClass = NameServiceProxy2.class)
@Proxy(proxyClass = NameServiceProxy1.class)
@Proxy(proxyClass = NameServiceProxy3.class)
@NameProxy
@Component
public class NameService {
    public String getName(String id) {
        return "name: " + id;
    }
}
