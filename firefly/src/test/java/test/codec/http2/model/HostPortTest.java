package test.codec.http2.model;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.firefly.codec.http2.model.HostPort;

@RunWith(Parameterized.class)
public class HostPortTest {
    @Parameters(name = "{0}")
    public static List<String[]> testCases() {
        String data[][] = new String[][]{
                {"host", "host", null},
                {"host:80", "host", "80"},
                {"10.10.10.1", "10.10.10.1", null},
                {"10.10.10.1:80", "10.10.10.1", "80"},
                {"[0::0::0::1]", "[0::0::0::1]", null},
                {"[0::0::0::1]:80", "[0::0::0::1]", "80"},

                {null, null, null},
                {"host:", null, null},
                {"", null, null},
                {":80", null, "80"},
                {"127.0.0.1:", null, null},
                {"[0::0::0::0::1]:", null, null},
                {"host:xxx", null, null},
                {"127.0.0.1:xxx", null, null},
                {"[0::0::0::0::1]:xxx", null, null},
                {"host:-80", null, null},
                {"127.0.0.1:-80", null, null},
                {"[0::0::0::0::1]:-80", null, null},
        };
        return Arrays.asList(data);
    }

    @Parameter(0)
    public String _authority;

    @Parameter(1)
    public String _expectedHost;

    @Parameter(2)
    public String _expectedPort;

    @Test
    public void test() {
        try {
            HostPort hostPort = new HostPort(_authority);
            assertThat(hostPort.getHost(), is(_expectedHost));

            if (_expectedPort == null)
                assertThat(hostPort.getPort(), is(0));
            else
                assertThat(hostPort.getPort(), is(Integer.valueOf(_expectedPort)));
        } catch (Exception e) {
            assertNull(_expectedHost);
        }
    }

}
