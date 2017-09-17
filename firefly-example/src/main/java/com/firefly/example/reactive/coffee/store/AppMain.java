package com.firefly.example.reactive.coffee.store;

import com.firefly.$;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.router.RouterInstaller;
import com.firefly.example.reactive.coffee.store.utils.DBUtils;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.utils.log.slf4j.ext.LazyLogger;

/**
 * @author Pengtao Qiu
 */
public class AppMain {

    private static final LazyLogger logger = LazyLogger.create();

    @Inject
    private HTTP2ServerBuilder server;

    @Inject
    private ProjectConfig config;

    @Inject
    private DBUtils dbUtils;

    public void initData() {
        dbUtils.createTables();
        dbUtils.initializeData();
    }

    public void installRouters() {
        $.getBeans(RouterInstaller.class).stream().sorted().forEach(installer -> {
            logger.info(() -> "install routers [" + installer.getClass().getName() + "]");
            installer.install();
        });
    }

    public void start(String[] args) {
        initData();
        installRouters();
        server.listen(config.getHost(), config.getPort());
    }

    public static void main(String[] args) {
        $.getBean(AppMain.class).start(args);
    }
}
