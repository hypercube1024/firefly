package test.http.router;

import com.firefly.server.http2.router.Matcher;
import com.firefly.server.http2.router.Router;
import com.firefly.server.http2.router.impl.RouterManagerImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.SortedSet;

import static org.hamcrest.Matchers.*;

/**
 * @author Pengtao Qiu
 */
public class TestMatcher {

    @Test
    public void testFindRouter() {
        RouterManagerImpl routerManager = new RouterManagerImpl();
        Router router0 = routerManager.register()
                                      .get("/hello/get")
                                      .produces("application/json");
        Router router1 = routerManager.register()
                                      .get("/hello/:param")
                                      .produces("*/json");

        SortedSet<RouterManagerImpl.RouterMatchResult> result = routerManager.findRouter("GET", "/hello/get", null, "application/json");
        Assert.assertThat(result, notNullValue());
        Assert.assertThat(result.size(), is(2));
        Assert.assertThat(result.first().getRouter(), is(router0));
        Assert.assertThat(result.last().getRouter(), is(router1));
        Assert.assertThat(result.last().getParameters().get("param"), is("get"));

        result = routerManager.findRouter("GET", "/hello/get", null, null);
        Assert.assertThat(result, empty());
    }

    @Test
    public void testMIMETypeMatcher() {
        RouterManagerImpl routerManager = new RouterManagerImpl();

        Router router0 = routerManager.register().consumes("text/html");
        Router router1 = routerManager.register().consumes("*/json");

        Matcher.MatchResult result = routerManager.getContentTypePreciseMatcher().match("text/html");
        Assert.assertThat(result, notNullValue());
        Assert.assertThat(result.getRouters().size(), is(1));
        Assert.assertThat(result.getRouters().contains(router0), is(true));

        result = routerManager.getContentTypePatternMatcher().match("application/json");
        Assert.assertThat(result, notNullValue());
        Assert.assertThat(result.getRouters().size(), is(1));
        Assert.assertThat(result.getRouters().contains(router1), is(true));
        Assert.assertThat(result.getParameters().get(router1).get("param0"), is("application"));
    }

    @Test
    public void testMethodMatcher() {
        RouterManagerImpl routerManager = new RouterManagerImpl();

        Router router0 = routerManager.register().post("/food/update");

        Matcher.MatchResult result = routerManager.getHttpMethodMatcher().match("POST");
        Assert.assertThat(result, notNullValue());
        Assert.assertThat(result.getRouters().size(), is(1));
        Assert.assertThat(result.getRouters().contains(router0), is(true));

        result = routerManager.getPrecisePathMather().match("/food/update");
        Assert.assertThat(result, notNullValue());
        Assert.assertThat(result.getRouters().size(), is(1));
        Assert.assertThat(result.getRouters().contains(router0), is(true));

        result = routerManager.getHttpMethodMatcher().match("GET");
        Assert.assertThat(result, nullValue());
    }


    @Test
    public void testPathMatcher() {
        RouterManagerImpl routerManager = new RouterManagerImpl();

        Router router0 = routerManager.register().path("/hello/foo");
        Router router1 = routerManager.register().path("/");
        Router router2 = routerManager.register().path("/hello*");
        Router router3 = routerManager.register().path("*");
        Router router4 = routerManager.register().path("/*");
        Router router5 = routerManager.register().path("/he*/*");
        Router router6 = routerManager.register().path("/hello/:foo");
        Router router7 = routerManager.register().path("/:hello/:foo/");
        Router router8 = routerManager.register().path("/hello/:foo/:bar");
        Router router9 = routerManager.register().pathRegex("/hello(\\d*)");

        Matcher.MatchResult result = routerManager.getPrecisePathMather().match("/hello/foo");
        Assert.assertThat(result, notNullValue());
        Assert.assertThat(result.getRouters().size(), is(1));
        Assert.assertThat(result.getRouters().contains(router0), is(true));

        result = routerManager.getPrecisePathMather().match("/");
        Assert.assertThat(result, notNullValue());
        Assert.assertThat(result.getRouters().size(), is(1));
        Assert.assertThat(result.getRouters().contains(router1), is(true));

        result = routerManager.getPatternPathMatcher().match("/hello/foo");
        Assert.assertThat(result, notNullValue());
        Assert.assertThat(result.getRouters().size(), is(4));
        Assert.assertThat(result.getRouters().contains(router2), is(true));
        Assert.assertThat(result.getParameters().get(router2).get("param0"), is("/foo"));
        Assert.assertThat(result.getRouters().contains(router3), is(true));
        Assert.assertThat(result.getParameters().get(router3).get("param0"), is("/hello/foo"));
        Assert.assertThat(result.getRouters().contains(router4), is(true));
        Assert.assertThat(result.getParameters().get(router4).get("param0"), is("hello/foo"));
        Assert.assertThat(result.getRouters().contains(router5), is(true));
        Assert.assertThat(result.getParameters().get(router5).get("param0"), is("llo"));
        Assert.assertThat(result.getParameters().get(router5).get("param1"), is("foo"));

        result = routerManager.getParameterPathMatcher().match("/hello/foooo");
        Assert.assertThat(result, notNullValue());
        Assert.assertThat(result.getRouters().size(), is(2));
        Assert.assertThat(result.getRouters().contains(router6), is(true));
        Assert.assertThat(result.getRouters().contains(router7), is(true));
        Assert.assertThat(result.getParameters().get(router6).get("foo"), is("foooo"));
        Assert.assertThat(result.getParameters().get(router7).get("foo"), is("foooo"));
        Assert.assertThat(result.getParameters().get(router7).get("hello"), is("hello"));

        result = routerManager.getParameterPathMatcher().match("/");
        Assert.assertThat(result, nullValue());

        result = routerManager.getParameterPathMatcher().match("/hello/11/2333");
        Assert.assertThat(result, notNullValue());
        Assert.assertThat(result.getRouters().size(), is(1));
        Assert.assertThat(result.getRouters().contains(router8), is(true));


        result = routerManager.getRegexPathMatcher().match("/hello113");
        Assert.assertThat(result, notNullValue());
        Assert.assertThat(result.getRouters().size(), is(1));
        Assert.assertThat(result.getRouters().contains(router9), is(true));
        Assert.assertThat(result.getParameters().get(router9).get("group1"), is("113"));
    }
}
