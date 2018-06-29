package test.oauth2;

import com.firefly.$;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.oauth2.as.issuer.OAuthIssuer;
import com.firefly.codec.oauth2.as.issuer.OAuthIssuerImpl;
import com.firefly.codec.oauth2.as.issuer.UUIDValueGenerator;
import com.firefly.codec.oauth2.exception.OAuthProblemException;
import com.firefly.codec.oauth2.model.*;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.utils.RandomUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private OAuthIssuer issuer;

    @Before
    public void init() {
        port = (int) RandomUtils.random(3000, 65534);
        url = "https://" + host + ":" + port;
        s = $.httpsServer();
        c = $.createHTTPsClient();
        issuer = new OAuthIssuerImpl(new UUIDValueGenerator());
        System.out.println("init");
    }

    @After
    public void destroy() {
        s.stop();
        c.stop();
        System.out.println("destroy");
    }

    @Test
    public void testCodeAuthorization() throws Exception {
        Map<String, AuthorizationRequest> codeMap = new ConcurrentHashMap<>();
        Map<String, AccessToken> accessTokenMap = new ConcurrentHashMap<>();
        Map<String, AccessToken> refreshTokenMap = new ConcurrentHashMap<>();

        s.router().get("/authorize").handler(ctx -> {
            try {
                AuthorizationRequest authReq = ctx.getAuthorizationRequest();
                System.out.println();
                System.out.println($.json.toJson(authReq));
                Assert.assertThat(authReq.getScope(), is("foo"));
                Assert.assertThat(authReq.getRedirectUri(), is("http://test.com/"));

                String code = issuer.authorizationCode();
                codeMap.put(code, authReq);
                ctx.redirectWithCode(code);
            } catch (OAuthProblemException e) {
                ctx.redirectAuthorizationError(e);
            }
        }).router().post("/accessToken").handler(ctx -> {
            try {
                AuthorizationCodeAccessTokenRequest codeReq = ctx.getAuthorizationCodeAccessTokenRequest();
                if (!codeMap.containsKey(codeReq.getCode())) {
                    throw OAuth.oauthProblem(OAuthError.TokenResponse.INVALID_GRANT).description("The code does not exist");
                }

                AccessTokenResponse tokenResponse = new AccessTokenResponse();
                tokenResponse.setAccessToken(issuer.accessToken());
                tokenResponse.setExpiresIn(3600L);
                tokenResponse.setRefreshToken(issuer.refreshToken());
                tokenResponse.setScope(codeMap.get(codeReq.getCode()).getScope());
                tokenResponse.setState(codeMap.get(codeReq.getCode()).getState());

                AccessToken accessToken = new AccessToken();
                $.javabean.copyBean(codeReq, accessToken);
                $.javabean.copyBean(tokenResponse, accessToken);
                accessToken.setCreateTime(new Date());
                accessTokenMap.put(tokenResponse.getAccessToken(), accessToken);
                refreshTokenMap.put(tokenResponse.getRefreshToken(), accessToken);

                // invalid code
                codeMap.remove(codeReq.getCode());
                ctx.writeAccessToken(tokenResponse).end();
            } catch (OAuthProblemException e) {
                ctx.writeAccessTokenError(e).end();
            }
        }).router().post("/refreshToken").handler(ctx -> {
            try {
                RefreshingTokenRequest req = ctx.getRefreshingTokenRequest();
                if (!refreshTokenMap.containsKey(req.getRefreshToken())) {
                    throw OAuth.oauthProblem(OAuthError.TokenResponse.INVALID_GRANT).description("The refreshing token does not exist");
                }

                // remove old access token
                AccessToken accessToken = refreshTokenMap.get(req.getRefreshToken());
                accessTokenMap.remove(accessToken.getAccessToken());
                accessTokenMap.remove(accessToken.getRefreshToken());

                // refresh access token
                AccessTokenResponse tokenResponse = new AccessTokenResponse();
                tokenResponse.setAccessToken(issuer.accessToken());
                tokenResponse.setExpiresIn(3600L);
                tokenResponse.setRefreshToken(issuer.refreshToken());
                tokenResponse.setScope(accessToken.getScope());
                tokenResponse.setState(accessToken.getState());

                $.javabean.copyBean(tokenResponse, accessToken);
                accessToken.setCreateTime(new Date());
                accessTokenMap.put(tokenResponse.getAccessToken(), accessToken);
                refreshTokenMap.put(tokenResponse.getRefreshToken(), accessToken);

                ctx.writeAccessToken(tokenResponse).end();
            } catch (OAuthProblemException e) {
                ctx.writeAccessTokenError(e).end();
            }
        }).router().get("/userInfo").handler(ctx -> {
            try {
                String token = ctx.getAccessToken();
                System.out.println("access token: " + token);
                if (!accessTokenMap.containsKey(token)) {
                    throw OAuth.oauthProblem(OAuthError.TokenResponse.INVALID_GRANT).description("The access token does not exist");
                }

                AccessToken accessToken = accessTokenMap.get(token);
                Long expiredTime = accessToken.getCreateTime().getTime() + (accessToken.getExpiresIn() * 1000);
                if (System.currentTimeMillis() > expiredTime) {
                    throw OAuth.oauthProblem(OAuthError.TokenResponse.INVALID_GRANT).description("The access token is expired");
                }

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
        Assert.assertThat(accessTokenMap.containsKey(tokenResponse.getAccessToken()), is(true));

        // refresh access token
        resp = c.post(url + "/refreshToken")
                .refreshTokenRequest(refreshToken(tokenResponse.getRefreshToken()).scope("foo").clientId("client1"))
                .submit().get();
        System.out.println(resp.getStatus());
        Assert.assertThat(resp.getStatus(), is(HttpStatus.OK_200));

        tokenResponse = resp.getJsonBody(AccessTokenResponse.class);
        System.out.println($.json.toJson(tokenResponse));
        Assert.assertThat(accessTokenMap.containsKey(tokenResponse.getAccessToken()), is(true));

        resp = c.get(url + "/userInfo?id=10&sign=dsf23")
                .putQueryParam(OAUTH_ACCESS_TOKEN, tokenResponse.getAccessToken())
                .submit().get();
        Assert.assertThat(resp.getStringBody(), is("Hello"));
    }
}
