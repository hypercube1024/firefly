---

category : docs
title: OAuth2 server and client

---

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Basic concepts](#basic-concepts)
- [Authorization Code Grant](#authorization-code-grant)
- [Implicit Grant](#implicit-grant)
- [Resource Owner Password Credentials Grant](#resource-owner-password-credentials-grant)
- [Client Credentials Grant](#client-credentials-grant)

<!-- /TOC -->

# Basic concepts
The OAuth 2.0 framework specifies several grant types for different use cases. The firefly framework provides APIs to support the four grant types, such as Authorization Code Grant, Implicit Grant, Resource Owner Password Credentials Grant, and Client Credentials Grant.

# Authorization Code Grant
The authorization code grant is redirection-based flow. We use the authorization code to exchange the access token, and then use the access token to get the protected resources.  

For convenience, we create the authorization service and resource service in the same process. The authorization server example:
```java
HTTP2ServerBuilder s = $.httpsServer();
s.router().get("/authorize").handler(ctx -> {
    try {
        AuthorizationRequest authReq = ctx.getAuthorizationRequest();

        System.out.println();
        System.out.println($.json.toJson(authReq));
        Assert.assertThat(authReq.getScope(), is("foo"));
        Assert.assertThat(authReq.getRedirectUri(), is("http://test.com/"));

        String code = authorizationService.generateCode(authReq);
        ctx.redirectWithCode(code);
    } catch (OAuthProblemException e) {
        ctx.redirectAuthorizationError(e);
    }
}).router().post("/accessToken").handler(ctx -> {
    try {
        AuthorizationCodeAccessTokenRequest codeReq = ctx.getAuthorizationCodeAccessTokenRequest();
        AccessTokenResponse tokenResponse = authorizationService.generateAccessToken(codeReq);
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
```

The authorization code grant flow:
1. The client visits the `/authorize` with the `clientId`, `redirectUri`, `scope`, and `state` parameters.
2. The authorization server redirects to the client specified URI.
3. The client gets the code from the redirection URI and sends the code to the `/accessToken`.
4. The authorization server responses the access token.
5. The client uses the access token to visit the protected resources.

The OAuth2 client example:
```java
SimpleHTTPClient c = $.createHTTPsClient();
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
```

# Implicit Grant
The implicit grant is also a redirection-based flow. It skips to get an authorization code and uses client id to exchange the access token directly.  

The authorization server example:
```java
HTTP2ServerBuilder s = $.httpsServer();
s.router().get("/authorize").handler(ctx -> {
    try {
        AuthorizationRequest authReq = ctx.getAuthorizationRequest();

        System.out.println();
        System.out.println($.json.toJson(authReq));
        Assert.assertThat(authReq.getScope(), is("foo"));
        Assert.assertThat(authReq.getRedirectUri(), is("http://test.com/"));

        AccessTokenResponse tokenResponse = authorizationService.generateAccessToken(authReq);
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
```

The implicit grant flow:
1. The client visits the `/authorize` with the `clientId`, `redirectUri`, `scope`, and `state` parameters.
2. The authorization server redirects to the client specified URI.
3. The client gets the access token from the redirection URI hash value.
4. The client uses the access token to visit the protected resources.

The OAuth2 client example:
```java
SimpleHTTPClient c = $.createHTTPsClient();
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
```

# Resource Owner Password Credentials Grant
The client uses the username and password to exchange the access token.  

The authorization server example:
```java
HTTP2ServerBuilder s = $.httpsServer();
s.router().get("/accessToken").handler(ctx -> {
    try {
        PasswordAccessTokenRequest request = ctx.getPasswordAccessTokenRequest();
        AccessTokenResponse tokenResponse = authorizationService.generateAccessToken(request);
        ctx.writeAccessToken(tokenResponse).end();
    } catch (OAuthProblemException e) {
        ctx.writeAccessTokenError(e).end();
    }
}).router().get("/userInfo").handler(ctx -> {
    try {
        String token = ctx.getAccessToken();
        authorizationService.verifyAccessToken(token);
        ctx.end("Hello password credential grant");
    } catch (OAuthProblemException e) {
        ctx.writeAccessTokenError(e).end();
    }
}).listen(host, port);
```

The resource owner password credentials grant flow:
1. The client visits the `/accessToken` with the `username`, `password`, and `scope` parameters.
2. The authorization server response the access token.
3. The client uses the access token to visit the protected resources.

The OAuth2 client example:
```java
SimpleHTTPClient c = $.createHTTPsClient();
SimpleResponse resp = c.get(url + "/accessToken")
                       .pwdAccessTokenRequest(usernameAndPassword("pt", "pt123").scope("bar"))
                       .submit().get();
System.out.println(resp.getStatus());
Assert.assertThat(resp.getStatus(), is(HttpStatus.OK_200));

AccessTokenResponse tokenResponse = resp.getJsonBody(AccessTokenResponse.class);
System.out.println($.json.toJson(tokenResponse));
Assert.assertThat(authorizationService.accessTokenMap.containsKey(tokenResponse.getAccessToken()), is(true));

resp = c.get(url + "/userInfo?id=10&sign=dsf23")
        .putQueryParam(OAUTH_ACCESS_TOKEN, tokenResponse.getAccessToken())
        .submit().get();
Assert.assertThat(resp.getStringBody(), is("Hello password credential grant"));
```

# Client Credentials Grant
The client uses the client id and secret to exchange the access token.  

The authorization server example:
```java
HTTP2ServerBuilder s = $.httpsServer();
s.router().get("/accessToken").handler(ctx -> {
    try {
        ClientCredentialAccessTokenRequest request = ctx.getClientCredentialAccessTokenRequest();
        AccessTokenResponse tokenResponse = authorizationService.generateAccessToken(request);
        ctx.writeAccessToken(tokenResponse).end();
    } catch (OAuthProblemException e) {
        ctx.writeAccessTokenError(e).end();
    }
}).router().get("/userInfo").handler(ctx -> {
    try {
        String token = ctx.getAccessToken();
        authorizationService.verifyAccessToken(token);
        ctx.end("Hello client credentials grant");
    } catch (OAuthProblemException e) {
        ctx.writeAccessTokenError(e).end();
    }
}).listen(host, port);
```

The client credentials grant flow:
1. The client visits the `/accessToken` with the `clientId`, `secret`, and `scope` parameters.
2. The authorization server response the access token.
3. The client uses the access token to visit the protected resources.

The OAuth2 client example:
```java
SimpleHTTPClient c = $.createHTTPsClient();
SimpleResponse resp = c.get(url + "/accessToken")
                       .credAccessTokenRequest(credAccessTokenRequest().scope("bar").clientId("pt123").clientSecret("1234565"))
                       .submit().get();
System.out.println(resp.getStatus());
Assert.assertThat(resp.getStatus(), is(HttpStatus.OK_200));

AccessTokenResponse tokenResponse = resp.getJsonBody(AccessTokenResponse.class);
System.out.println($.json.toJson(tokenResponse));
Assert.assertThat(authorizationService.accessTokenMap.containsKey(tokenResponse.getAccessToken()), is(true));

resp = c.get(url + "/userInfo?id=10&sign=dsf23")
        .putQueryParam(OAUTH_ACCESS_TOKEN, tokenResponse.getAccessToken())
        .submit().get();
Assert.assertThat(resp.getStringBody(), is("Hello client credentials grant"));
```
