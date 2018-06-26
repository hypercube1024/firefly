package test.codec.oauth;

import com.firefly.codec.oauth2.model.*;
import com.firefly.codec.oauth2.model.message.types.ResponseType;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestOauth {

    @Test
    public void testAuthRequest() {
        AuthorizationRequest.Builder builder = OAuth.authRequest()
                                                    .responseType(ResponseType.CODE.toString())
                                                    .clientId("321")
                                                    .redirectUri("https://www.test1.com")
                                                    .scope("foo")
                                                    .state("a1")
                                                    .put("x1", "x1-1")
                                                    .add("x2", "x2-1")
                                                    .add("x2", "x2-2");
        String param = builder.toEncodedUrl();
        System.out.println(param);
        Assert.assertThat(param, is("scope=foo&x1=x1%2D1&response%5Ftype=code&x2=x2%2D1&x2=x2%2D2&redirect%5Furi=https%3A%2F%2Fwww%2Etest1%2Ecom&state=a1&client%5Fid=321"));

        String json = builder.toJson();
        System.out.println(json);
        Assert.assertThat(json, is("{\"scope\":\"foo\",\"x1\":\"x1-1\",\"response_type\":\"code\",\"x2\":[\"x2-1\",\"x2-2\"],\"redirect_uri\":\"https:\\/\\/www.test1.com\",\"state\":\"a1\",\"client_id\":\"321\"}"));
    }

    @Test
    public void testCodeRequest() {
        AuthorizationCodeAccessTokenRequest.Builder builder = OAuth.codeAccessTokenRequest()
                                                                   .code("123")
                                                                   .clientId("321")
                                                                   .redirectUri("https://www.test1.com")
                                                                   .put("x1", "x1");
        String param = builder.toEncodedUrl();
        System.out.println(param);
        Assert.assertThat(param, is("code=123&grant%5Ftype=authorization%5Fcode&x1=x1&redirect%5Furi=https%3A%2F%2Fwww%2Etest1%2Ecom&client%5Fid=321"));

        String json = builder.toJson();
        System.out.println(json);
        Assert.assertThat(json, is("{\"code\":\"123\",\"grant_type\":\"authorization_code\",\"x1\":\"x1\",\"redirect_uri\":\"https:\\/\\/www.test1.com\",\"client_id\":\"321\"}"));
    }

    @Test
    public void testPwdRequest() {
        PasswordAccessTokenRequest.Builder builder = OAuth.pwdAccessTokenRequest()
                                                          .username("Alvin")
                                                          .password("12345")
                                                          .scope("foo");
        String param = builder.toEncodedUrl();
        System.out.println(param);
        Assert.assertThat(param, is("password=12345&grant%5Ftype=password&scope=foo&username=Alvin"));

        String json = builder.toJson();
        System.out.println(json);
        Assert.assertThat(json, is("{\"password\":\"12345\",\"grant_type\":\"password\",\"scope\":\"foo\",\"username\":\"Alvin\"}"));
    }

    @Test
    public void testCredRequest() {
        ClientCredentialAccessTokenRequest.Builder builder = OAuth.credAccessTokenRequest()
                                                                  .clientId("111")
                                                                  .clientSecret("dsfsfsfsf")
                                                                  .scope("bar");
        String param = builder.toEncodedUrl();
        System.out.println(param);
        Assert.assertThat(param, is("grant%5Ftype=client%5Fcredentials&scope=bar&client%5Fsecret=dsfsfsfsf&client%5Fid=111"));

        String json = builder.toJson();
        System.out.println(json);
        Assert.assertThat(json, is("{\"grant_type\":\"client_credentials\",\"scope\":\"bar\",\"client_secret\":\"dsfsfsfsf\",\"client_id\":\"111\"}"));
    }

}
