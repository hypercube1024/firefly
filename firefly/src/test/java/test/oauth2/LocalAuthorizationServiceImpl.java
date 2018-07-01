package test.oauth2;

import com.firefly.$;
import com.firefly.codec.oauth2.as.issuer.OAuthIssuer;
import com.firefly.codec.oauth2.as.issuer.OAuthIssuerImpl;
import com.firefly.codec.oauth2.as.issuer.UUIDValueGenerator;
import com.firefly.codec.oauth2.as.service.AuthorizationService;
import com.firefly.codec.oauth2.model.*;
import com.firefly.codec.oauth2.model.message.types.ResponseType;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalAuthorizationServiceImpl implements AuthorizationService {

    Map<String, AuthorizationRequest> codeMap = new ConcurrentHashMap<>();
    Map<String, AccessToken> accessTokenMap = new ConcurrentHashMap<>();
    Map<String, AccessToken> refreshTokenMap = new ConcurrentHashMap<>();

    private OAuthIssuer issuer;

    public LocalAuthorizationServiceImpl() {
        issuer = new OAuthIssuerImpl(new UUIDValueGenerator());
    }

    public String generateCode(AuthorizationRequest request) {
        if (!request.getResponseType().equals(ResponseType.CODE.toString())) {
            throw OAuth.oauthProblem(OAuthError.CodeResponse.UNSUPPORTED_RESPONSE_TYPE).description("The response type must be 'code'");
        }

        String code = issuer.authorizationCode();
        codeMap.put(code, request);
        return code;
    }

    public AccessTokenResponse generateAccessToken(AuthorizationCodeAccessTokenRequest request) {
        if (!codeMap.containsKey(request.getCode())) {
            throw OAuth.oauthProblem(OAuthError.TokenResponse.INVALID_GRANT).description("The code does not exist");
        }

        AccessTokenResponse tokenResponse = new AccessTokenResponse();
        tokenResponse.setAccessToken(issuer.accessToken());
        tokenResponse.setExpiresIn(3600L);
        tokenResponse.setRefreshToken(issuer.refreshToken());
        tokenResponse.setScope(codeMap.get(request.getCode()).getScope());
        tokenResponse.setState(codeMap.get(request.getCode()).getState());

        AccessToken accessToken = new AccessToken();
        $.javabean.copyBean(request, accessToken);
        $.javabean.copyBean(tokenResponse, accessToken);
        accessToken.setCreateTime(new Date());
        accessTokenMap.put(tokenResponse.getAccessToken(), accessToken);
        refreshTokenMap.put(tokenResponse.getRefreshToken(), accessToken);

        // invalid code
        codeMap.remove(request.getCode());
        return tokenResponse;
    }

    public AccessTokenResponse generateAccessToken(AuthorizationRequest request) {
        if (!request.getResponseType().equals(ResponseType.TOKEN.toString())) {
            throw OAuth.oauthProblem(OAuthError.CodeResponse.UNSUPPORTED_RESPONSE_TYPE).description("The response type must be 'token'");
        }

        return generateAccessToken(request.getScope(), request.getState());
    }

    @Override
    public AccessTokenResponse generateAccessToken(PasswordAccessTokenRequest request) {
        return generateAccessToken(request.getScope(), null);
    }


    @Override
    public AccessTokenResponse generateAccessToken(ClientCredentialAccessTokenRequest request) {
        return generateAccessToken(request.getScope(), null);
    }

    protected AccessTokenResponse generateAccessToken(String scope, String state) {
        AccessTokenResponse tokenResponse = new AccessTokenResponse();
        tokenResponse.setAccessToken(issuer.accessToken());
        tokenResponse.setExpiresIn(3600L);
        tokenResponse.setRefreshToken(issuer.refreshToken());
        tokenResponse.setScope(scope);
        tokenResponse.setState(state);

        AccessToken accessToken = new AccessToken();
        $.javabean.copyBean(tokenResponse, accessToken);
        accessToken.setCreateTime(new Date());
        accessTokenMap.put(tokenResponse.getAccessToken(), accessToken);
        refreshTokenMap.put(tokenResponse.getRefreshToken(), accessToken);
        return tokenResponse;
    }

    public AccessTokenResponse refreshToken(RefreshingTokenRequest request) {
        if (!refreshTokenMap.containsKey(request.getRefreshToken())) {
            throw OAuth.oauthProblem(OAuthError.TokenResponse.INVALID_GRANT).description("The refreshing token does not exist");
        }

        // remove old access token
        AccessToken accessToken = refreshTokenMap.get(request.getRefreshToken());
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
        return tokenResponse;
    }

    public void verifyAccessToken(String token) {
        if (!accessTokenMap.containsKey(token)) {
            throw OAuth.oauthProblem(OAuthError.TokenResponse.INVALID_GRANT).description("The access token does not exist");
        }

        AccessToken accessToken = accessTokenMap.get(token);
        Long expiredTime = accessToken.getCreateTime().getTime() + (accessToken.getExpiresIn() * 1000);
        if (System.currentTimeMillis() > expiredTime) {
            accessTokenMap.remove(token);
            throw OAuth.oauthProblem(OAuthError.TokenResponse.INVALID_GRANT).description("The access token is expired");
        }
    }
}
