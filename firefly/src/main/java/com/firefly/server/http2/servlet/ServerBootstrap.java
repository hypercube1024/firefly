package com.firefly.server.http2.servlet;

import com.firefly.mvc.web.WebContext;
import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.server.http2.servlet.model.JVMProcess;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.json.Json;
import com.firefly.utils.lang.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;

public class ServerBootstrap extends AbstractLifeCycle {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private final HTTP2Server http2Server;
    private final long createTime = System.currentTimeMillis();
    private File tempDir;

    public ServerBootstrap(ServerHTTP2Configuration http2Configuration) {
        WebContext context = new ServerAnnotationWebContext(http2Configuration);
        this.http2Server = new HTTP2Server(http2Configuration.getHost(), http2Configuration.getPort(),
                http2Configuration,
                new ServletServerHTTPHandler(http2Configuration, new HttpServletDispatcherController(context)));
    }

    public ServerBootstrap(String configFileName) {
        if (VerifyUtils.isEmpty(configFileName)) {
            configFileName = ServerHTTP2Configuration.DEFAULT_CONFIG_FILE_NAME;
        }

        WebContext context = new ServerAnnotationWebContext(configFileName);
        ServerHTTP2Configuration http2Configuration = context.getBean(ServerHTTP2Configuration.class);
        this.http2Server = new HTTP2Server(http2Configuration.getHost(), http2Configuration.getPort(),
                http2Configuration,
                new ServletServerHTTPHandler(http2Configuration, new HttpServletDispatcherController(context)));
    }

    public ServerBootstrap(String host, int port) {
        this(null, host, port);
    }

    public ServerBootstrap(String configFileName, String host, int port) {
        if (VerifyUtils.isEmpty(configFileName)) {
            configFileName = ServerHTTP2Configuration.DEFAULT_CONFIG_FILE_NAME;
        }

        WebContext context = new ServerAnnotationWebContext(configFileName);
        ServerHTTP2Configuration http2Configuration = context.getBean(ServerHTTP2Configuration.class);
        if (http2Configuration == null) {
            http2Configuration = new ServerHTTP2Configuration();
        }

        http2Configuration.setHost(host);
        http2Configuration.setPort(port);
        this.http2Server = new HTTP2Server(http2Configuration.getHost(), http2Configuration.getPort(),
                http2Configuration,
                new ServletServerHTTPHandler(http2Configuration, new HttpServletDispatcherController(context)));
    }

    public File getTempDir() {
        return tempDir;
    }

    @Override
    protected void init() {
        File file = new File(((ServerHTTP2Configuration) http2Server.getHttp2Configuration()).getTemporaryDirectory());
        if (!file.exists()) {
            file.mkdirs();
        }
        tempDir = file;
        log.info("the temp dir is {}", file.getAbsolutePath());

        http2Server.start();

        new Thread(() -> {
            RuntimeMXBean b = ManagementFactory.getRuntimeMXBean();
            String jvmName = b.getName();
            log.info("the jvm name is {}", jvmName);
            JVMProcess p = new JVMProcess();
            p.setName(jvmName);
            p.setHost(http2Server.getHost());
            p.setPort(http2Server.getPort());

            File processFile = new File(file, "_current_firefly_process_info");
            processFile.deleteOnExit();
            try (FileOutputStream out = new FileOutputStream(processFile)) {
                out.write(Json.toJson(p).getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.error("write jvm process info exception", e);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("the process " + jvmName + " will stop");
                this.stop();
            }, "the firefly shutdown thread"));
        }, "the firefly jvm process info writing thread").start();

        log.info("the server start spends time in {} ms", System.currentTimeMillis() - createTime);
    }

    @Override
    protected void destroy() {
        AsyncContextImpl.shutdown();
        http2Server.stop();
        ((ServerHTTP2Configuration) http2Server.getHttp2Configuration()).getHttpSessionManager().stop();
    }

}
