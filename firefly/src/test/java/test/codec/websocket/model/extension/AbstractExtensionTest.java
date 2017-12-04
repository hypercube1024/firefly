package test.codec.websocket.model.extension;

import com.firefly.codec.websocket.stream.WebSocketPolicy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

public abstract class AbstractExtensionTest
{
    @Rule
    public TestName testname = new TestName();

    protected ExtensionTool clientExtensions;
    protected ExtensionTool serverExtensions;

    @Before
    public void init()
    {
        clientExtensions = new ExtensionTool(WebSocketPolicy.newClientPolicy());
        serverExtensions = new ExtensionTool(WebSocketPolicy.newServerPolicy());
    }
}
