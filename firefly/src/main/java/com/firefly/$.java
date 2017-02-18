package com.firefly;

import com.firefly.client.http2.HTTP2ClientSingleton;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleHTTPClientConfiguration;
import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.SimpleTcpServer;
import com.firefly.net.tcp.TcpConfiguration;
import com.firefly.net.tcp.TcpServerConfiguration;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.SimpleHTTPServer;
import com.firefly.server.http2.SimpleHTTPServerConfiguration;
import com.firefly.server.http2.router.handler.body.HTTPBodyConfiguration;
import com.firefly.utils.StringUtils;
import com.firefly.utils.concurrent.ThreadUtils;
import com.firefly.utils.io.IO;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;
import com.firefly.utils.lang.URIUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

/**
 * Unsorted utilities. The main functions of Firefly start from here.
 *
 * @author Pengtao Qiu
 */
public interface $ {


    /**
     * Use the singleton HTTP client to send all requests.
     * The HTTP client automatically manages HTTP connection in the BoundedAsynchronousPool.
     *
     * @return the HTTP client singleton instance.
     */
    static SimpleHTTPClient httpClient() {
        return HTTP2ClientSingleton.getInstance().httpClient();
    }

    static HTTP2ClientSingleton httpClientSingleton() {
        return HTTP2ClientSingleton.getInstance();
    }

    /**
     * Create an new HTTP client instance
     *
     * @return new HTTP client instance
     */
    static SimpleHTTPClient createHTTPClient() {
        return new SimpleHTTPClient();
    }

    static SimpleHTTPClient createHTTPClient(SimpleHTTPClientConfiguration configuration) {
        return new SimpleHTTPClient(configuration);
    }

    /**
     * Create an new HTTP server instance
     *
     * @return new HTTP server instance
     */
    static SimpleHTTPServer createHTTPServer() {
        return new SimpleHTTPServer();
    }

    static SimpleHTTPServer createHTTPServer(SimpleHTTPServerConfiguration configuration) {
        return new SimpleHTTPServer(configuration);
    }

    static SimpleTcpClient createTCPClient() {
        return new SimpleTcpClient();
    }

    static SimpleTcpClient createTCPClient(TcpConfiguration configuration) {
        return new SimpleTcpClient(configuration);
    }

    static SimpleTcpServer createTCPServer() {
        return new SimpleTcpServer();
    }

    static SimpleTcpServer createTCPServer(TcpServerConfiguration configuration) {
        return new SimpleTcpServer(configuration);
    }

    /**
     * Use fluent API to create an new HTTP server instance.
     * for example:
     * <pre>
     * $.httpServer()
     * .router().get("/").handler(ctx -> ctx.write("hello world! ").next())
     * .router().get("/").handler(ctx -> ctx.end("end message"))
     * .listen("localhost", 8080);
     * </pre>
     *
     * @return HTTP server builder API
     */
    static HTTP2ServerBuilder httpServer() {
        return new HTTP2ServerBuilder().httpServer();
    }

    static HTTP2ServerBuilder httpServer(SimpleHTTPServerConfiguration serverConfiguration,
                                         HTTPBodyConfiguration httpBodyConfiguration) {
        return new HTTP2ServerBuilder().httpServer(serverConfiguration, httpBodyConfiguration);
    }

    static HTTP2ServerBuilder emptyHttpServer() {
        return new HTTP2ServerBuilder().emptyHttpServer();
    }

    static HTTP2ServerBuilder emptyHttpServer(SimpleHTTPServerConfiguration serverConfiguration) {
        return new HTTP2ServerBuilder().emptyHttpServer(serverConfiguration);
    }

    interface io {
        static void close(Closeable closeable) {
            IO.close(closeable);
        }

        static String toString(InputStream in) {
            return toString(in, "UTF-8");
        }

        static String toString(InputStream in, String encoding) {
            try {
                return IO.toString(in, encoding);
            } catch (IOException e) {
                return null;
            }
        }

        static String toString(Reader in) {
            try {
                return IO.toString(in);
            } catch (IOException e) {
                return null;
            }
        }

        static byte[] readBytes(InputStream in) {
            try {
                return IO.readBytes(in);
            } catch (IOException e) {
                return null;
            }
        }
    }

    interface thread {
        static void sleep(long millis) {
            ThreadUtils.sleep(millis);
        }
    }

    interface json {
        static String toJson(Object obj) {
            return Json.toJson(obj);
        }

        static <T> T parse(String json, Class<T> clazz) {
            return Json.toObject(json, clazz);
        }

        static JsonObject parseToObject(String json) {
            return Json.toJsonObject(json);
        }

        static JsonArray parseToArray(String json) {
            return Json.toJsonArray(json);
        }
    }

    interface string {
        static boolean hasText(String str) {
            return StringUtils.hasText(str);
        }

        static String[] split(String str, String separatorChars) {
            return StringUtils.split(str, separatorChars);
        }

        static String[] split(String str, char separatorChar) {
            return StringUtils.split(str, separatorChar);
        }

        static String replace(String s, Map<String, Object> map) {
            return StringUtils.replace(s, map);
        }

        static String replace(String s, Object... objs) {
            return StringUtils.replace(s, objs);
        }

        static String escapeXML(String str) {
            return StringUtils.escapeXML(str);
        }

        static byte[] getBytes(String s, String charset) {
            return StringUtils.getBytes(s, charset);
        }

        static byte[] getBytes(String s) {
            return StringUtils.getUtf8Bytes(s);
        }
    }

    interface uri {
        static StringBuilder newURIBuilder(String scheme, String server, int port) {
            return URIUtils.newURIBuilder(scheme, server, port);
        }

        static String newURI(String scheme, String server, int port, String path, String query) {
            return URIUtils.newURI(scheme, server, port, path, query);
        }

        static UrlEncoded encode() {
            return new UrlEncoded();
        }
    }
}
