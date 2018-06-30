package test.oauth2;

import com.firefly.$;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.oauth2.exception.OAuthProblemException;
import com.firefly.codec.oauth2.model.*;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.utils.RandomUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.firefly.codec.oauth2.model.OAuth.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Pengtao Qiu
 */
public class TestOAuth2ServerAndClient {

    private int port;
    private String host = "localhost";
    private String url;
    private SimpleHTTPClient c;
    private HTTP2ServerBuilder s;
    private AuthorizationService authorizationService;

    @Before
    public void init() {
        port = (int) RandomUtils.random(3000, 65534);
        url = "https://" + host + ":" + port;
        s = $.httpsServer();
        c = $.createHTTPsClient();
        authorizationService = new AuthorizationService();
        System.out.println("init");
    }

    @After
    public void destroy() {
        s.stop();
        c.stop();
        System.out.println("destroy");
    }

    @Test
    public void testAuthorizationCodeGrant() throws Exception {
        s.router().get("/authorize").handler(ctx -> {
            try {
                AuthorizationRequest authReq = ctx.getAuthorizationRequest();

                System.out.println();
                System.out.println($.json.toJson(authReq));
                Assert.assertThat(authReq.getScope(), is("foo"));
                Assert.assertThat(authReq.getRedirectUri(), is("http://test.com/"));

                String code = authorizationService.getCode(authReq);
                ctx.redirectWithCode(code);
            } catch (OAuthProblemException e) {
                ctx.redirectAuthorizationError(e);
            }
        }).router().post("/accessToken").handler(ctx -> {
            try {
                AuthorizationCodeAccessTokenRequest codeReq = ctx.getAuthorizationCodeAccessTokenRequest();
                AccessTokenResponse tokenResponse = authorizationService.getAccessToken(codeReq);
                ctx.writeAccessToken(tokenResponse).end();
            } catch (OAuthProblemException e) {
                ctx.writeAccessTokenError(e).end();
            }
        }).router().post("/refreshToken").handler(ctx -> {
            try {
                RefreshingTokenRequest req = ctx.getRefreshingTokenRequest();
                AccessTokenResponse tokenResponse = authorizationService.refreshToken(req);
                ctx.writeAccessToken(tokenResponse).end();
            } catch (OAuthProblemException e) {
                ctx.writeAccessTokenError(e).end();
            }
        }).router().get("/userInfo").handler(ctx -> {
            try {
                String token = ctx.getAccessToken();
                authorizationService.verifyAccessToken(token);
                ctx.end("Hello");
            } catch (OAuthProblemException e) {
                ctx.writeAccessTokenError(e).end();
            }
        }).listen(host, port);

        // get the authorization code
        SimpleResponse resp = c.get(url + "/authorize")
                               .authRequest(codeRequest().clientId("client1").redirectUri("http://test.com/").scope("foo").state("index"))
                               .submit().get();
        System.out.println(resp.getStatus());
        Assert.assertThat(resp.getStatus(), is(HttpStatus.FOUND_302));

        String location = resp.getFields().get(HttpHeader.LOCATION);
        System.out.println(location);

        AuthorizationCodeResponse codeResponse = resp.getAuthorizationCodeResponse();
        Assert.assertThat(codeResponse, notNullValue());
        Assert.assertThat(codeResponse.getState(), is("index"));

        // get the access token
        resp = c.post(url + "/accessToken")
                .codeAccessTokenRequest(code(codeResponse.getCode()).redirectUri("http://test.com/").clientId("client1"))
                .submit().get();
        System.out.println(resp.getStatus());
        Assert.assertThat(resp.getStatus(), is(HttpStatus.OK_200));

        AccessTokenResponse tokenResponse = resp.getJsonBody(AccessTokenResponse.class);
        System.out.println($.json.toJson(tokenResponse));
        Assert.assertThat(authorizationService.accessTokenMap.containsKey(tokenResponse.getAccessToken()), is(true));

        // refresh access token
        resp = c.post(url + "/refreshToken")
                .refreshTokenRequest(refreshToken(tokenResponse.getRefreshToken()).scope("foo").clientId("client1"))
                .submit().get();
        System.out.println(resp.getStatus());
        Assert.assertThat(resp.getStatus(), is(HttpStatus.OK_200));

        tokenResponse = resp.getJsonBody(AccessTokenResponse.class);
        System.out.println($.json.toJson(tokenResponse));
        Assert.assertThat(authorizationService.accessTokenMap.containsKey(tokenResponse.getAccessToken()), is(true));

        resp = c.get(url + "/userInfo?id=10&sign=dsf23")
                .putQueryParam(OAUTH_ACCESS_TOKEN, tokenResponse.getAccessToken())
                .submit().get();
        Assert.assertThat(resp.getStringBody(), is("Hello"));
    }

    @Test
    public void testImplicitGrant() throws Exception {
        s.router().get("/authorize").handler(ctx -> {
            try {
                AuthorizationRequest authReq = ctx.getAuthorizationRequest();

                System.out.println();
                System.out.println($.json.toJson(authReq));
                Assert.assertThat(authReq.getScope(), is("foo"));
                Assert.assertThat(authReq.getRedirectUri(), is("http://test.com/"));

                AccessTokenResponse tokenResponse = authorizationService.getAccessToken(authReq);
                ctx.redirectAccessToken(tokenResponse);
            } catch (OAuthProblemException e) {
                ctx.redirectAuthorizationError(e);
            }
        }).router().get("/userInfo").handler(ctx -> {
            try {
                String token = ctx.getAccessToken();
                authorizationService.verifyAccessToken(token);
                ctx.end("Hello implicit grant");
            } catch (OAuthProblemException e) {
                ctx.writeAccessTokenError(e).end();
            }
        }).listen(host, port);

        // get the authorization code
        SimpleResponse resp = c.get(url + "/authorize")
                               .authRequest(tokenRequest().clientId("client1").redirectUri("http://test.com/").scope("foo").state("index"))
                               .submit().get();
        System.out.println(resp.getStatus());
        Assert.assertThat(resp.getStatus(), is(HttpStatus.FOUND_302));

        String location = resp.getFields().get(HttpHeader.LOCATION);
        System.out.println(location);

        AccessTokenResponse token = resp.getAccessTokenResponseFromFragment();
        System.out.println("client received: " + $.json.toJson(token));
        Assert.assertThat(authorizationService.accessTokenMap.containsKey(token.getAccessToken()), is(true));

        resp = c.get(url + "/userInfo?id=10&sign=dsf23")
                .putQueryParam(OAUTH_ACCESS_TOKEN, token.getAccessToken())
                .submit().get();
        Assert.assertThat(resp.getStringBody(), is("Hello implicit grant"));
    }
}
