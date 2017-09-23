package com.firefly.example.reactive.coffee.store.router.impl.sys;

import com.firefly.$;
import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.db.RecordNotFound;
import com.firefly.example.reactive.coffee.store.ProjectConfig;
import com.firefly.example.reactive.coffee.store.service.UserService;
import com.firefly.example.reactive.coffee.store.vo.UserInfo;
import com.firefly.server.http2.router.HTTPSession;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.log.slf4j.ext.LazyLogger;
import com.firefly.utils.pattern.Pattern;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Pengtao Qiu
 */
@Component("loginHandler")
public class LoginHandler implements Handler {

    private static final LazyLogger logger = LazyLogger.create();

    private List<Pattern> uriWhitelist = Stream.of(
            "/",
            "/favicon.ico",
            "/static/*",
            "/products").map(p -> Pattern.compile(p, "*")).collect(Collectors.toList());

    @Inject
    private ProjectConfig config;

    @Inject
    private ErrorRenderer errorRenderer;

    @Inject
    private UserService userService;

    @Override
    public void handle(RoutingContext ctx) {
        Mono.fromFuture(ctx.getSession()).subscribe(session -> {
            try {
                String path = ctx.getURI().getPath();
                if (path.equals(config.getLoginURL())) {
                    switch (HttpMethod.fromString(ctx.getMethod())) {
                        case GET:
                            renderLoginPage(ctx);
                            break;
                        case POST:
                            verifyPasswordRequest(ctx, session);
                            break;
                        default:
                            errorRenderer.renderError(ctx, HttpStatus.METHOD_NOT_ALLOWED_405);
                    }
                } else if (path.equals(config.getLogoutURL())) {
                    logout(ctx, session);
                } else {
                    verifyLogin(ctx, session);
                }
            } catch (Exception e) {
                ctx.fail(e);
            }
        }, ctx::fail);
    }

    private void logout(RoutingContext ctx, HTTPSession session) {
        String backURL = ctx.getParamOpt("backURL").filter($.string::hasText).orElse("/");
        session.getAttributes().remove(config.getLoginUserKey());
        Mono.fromFuture(ctx.updateSession(session)).subscribe(ret -> {
            ctx.removeAttribute(config.getLoginUserKey());
            ctx.redirect(backURL);
            ctx.succeed(true);
            logger.info(() -> "logout success!");
        }, ctx::fail);
    }

    private void renderLoginPage(RoutingContext ctx) {
        String backURL = ctx.getParameter("backURL");
        Map<String, String> map = new HashMap<>();
        map.put("backURL", backURL);
        ctx.renderTemplate(config.getTemplateRoot() + "/login.mustache", map);
        ctx.succeed(true);
    }

    private void verifyPasswordRequest(RoutingContext ctx, HTTPSession session) {
        String username = ctx.getParameter("username");
        String password = ctx.getParameter("password");

        if (!$.string.hasText(username)) {
            ctx.fail(new IllegalArgumentException("The username is required"));
            return;
        }

        if (!$.string.hasText(password)) {
            ctx.fail(new IllegalArgumentException("The password is required"));
            return;
        }

        userService.getByName(username).subscribe(user -> {
            if (!user.getPassword().equals(password)) {
                ctx.fail(new IllegalArgumentException("The password is incorrect"));
            } else {
                String backURL = ctx.getParamOpt("backURL").filter($.string::hasText).orElse("/");
                UserInfo userInfo = new UserInfo();
                $.javabean.copyBean(user, userInfo);
                session.getAttributes().put(config.getLoginUserKey(), userInfo);
                Mono.fromFuture(ctx.updateSession(session)).subscribe(ret -> {
                    ctx.setAttribute(config.getLoginUserKey(), userInfo);
                    ctx.redirect(backURL);
                    ctx.succeed(true);
                    logger.info(() -> "user " + userInfo + " login success!");
                }, ctx::fail);
            }
        }, ex -> {
            if (ex instanceof RecordNotFound || ex.getCause() instanceof RecordNotFound) {
                ctx.fail(new IllegalArgumentException("The username is incorrect"));
            } else {
                ctx.fail(ex);
            }
        });
    }

    private void verifyLogin(RoutingContext ctx, HTTPSession session) {
        UserInfo userInfo = (UserInfo) session.getAttributes().get(config.getLoginUserKey());
        if (skipVerify(ctx.getURI().getPath())) {
            Optional.ofNullable(userInfo).ifPresent(u -> ctx.setAttribute(config.getLoginUserKey(), u));
            ctx.next();
            return;
        }

        if (userInfo != null) {
            ctx.setAttribute(config.getLoginUserKey(), userInfo);
            ctx.next();
        } else {
            UrlEncoded urlEncoded = new UrlEncoded();
            urlEncoded.put("backURL", ctx.getURI().getPathQuery());
            String param = urlEncoded.encode(StandardCharsets.UTF_8, true);
            ctx.redirect(config.getLoginURL() + "?" + param);
            ctx.succeed(true);
        }
    }

    private boolean skipVerify(String uri) {
        return uriWhitelist.parallelStream().anyMatch(p -> p.match(uri) != null);
    }
}
