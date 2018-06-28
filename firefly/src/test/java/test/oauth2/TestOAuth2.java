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

import static com.firefly.codec.oauth2.model.OAuth.code;
import static com.firefly.codec.oauth2.model.OAuth.codeRequest;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Pengtao Qiu
 */
public class TestOAuth2 {

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
        Map<String, AccessTokenResponse> accessTokenMap = new ConcurrentHashMap<>();

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
                tokenResponse.setCreateTime(new Date());
                accessTokenMap.put(tokenResponse.getAccessToken(), tokenResponse);
                ctx.writeAccessToken(tokenResponse).end();
            } catch (OAuthProblemException e) {
                ctx.writeAccessTokenError(e).end();
            }
        }).router().get("/refreshToken").handler(ctx -> {

        }).listen(host, port);

        SimpleResponse resp = c.get(url + "/authorize")
                               .authRequest(codeRequest()
                                       .clientId("client1")
                                       .redirectUri("http://test.com/")
                                       .scope("foo")
                                       .state("index"))
                               .submit().get();

        System.out.println(resp.getStatus());
        Assert.assertThat(resp.getStatus(), is(HttpStatus.FOUND_302));

        String location = resp.getFields().get(HttpHeader.LOCATION);
        System.out.println(location);

        AuthorizationCodeResponse codeResponse = resp.getAuthorizationCodeResponse();
        Assert.assertThat(codeResponse, notNullValue());
        Assert.assertThat(codeResponse.getState(), is("index"));

        resp = c.post(url + "/accessToken")
                .codeAccessTokenRequest(code(codeResponse.getCode())
                        .redirectUri("http://test.com/")
                        .clientId("client1")).submit().get();

        System.out.println(resp.getStatus());
        Assert.assertThat(resp.getStatus(), is(HttpStatus.OK_200));

        AccessTokenResponse tokenResponse = resp.getJsonBody(AccessTokenResponse.class);
        System.out.println($.json.toJson(tokenResponse));
        Assert.assertThat(accessTokenMap.containsKey(tokenResponse.getAccessToken()), is(true));
    }
}
