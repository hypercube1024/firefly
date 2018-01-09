package com.firefly;

import com.firefly.client.http2.*;
import com.firefly.client.websocket.SimpleWebSocketClient;
import com.firefly.client.websocket.WebSocketClientSingleton;
import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.core.ApplicationContext;
import com.firefly.core.ApplicationContextSingleton;
import com.firefly.core.XmlApplicationContext;
import com.firefly.net.SecureSessionFactory;
import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.SimpleTcpServer;
import com.firefly.net.tcp.TcpConfiguration;
import com.firefly.net.tcp.TcpServerConfiguration;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.SimpleHTTPServer;
import com.firefly.server.http2.SimpleHTTPServerConfiguration;
import com.firefly.server.http2.router.handler.body.HTTPBodyConfiguration;
import com.firefly.utils.BeanUtils;
import com.firefly.utils.StringUtils;
import com.firefly.utils.concurrent.ThreadUtils;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.io.IO;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;
import com.firefly.utils.lang.GenericTypeReference;
import com.firefly.utils.lang.URIUtils;
import com.firefly.utils.lang.bean.PropertyAccess;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Unsorted utilities. The main functions of Firefly start from here.
 *
 * @author Pengtao Qiu
 */
public interface $ {


    /**
     * The singleton HTTP client to send all requests.
     * The HTTP client manages HTTP connection in the BoundedAsynchronousPool automatically.
     * The default protocol is HTTP 1.1.
     *
     * @return HTTP client singleton instance.
     */
    static SimpleHTTPClient httpClient() {
        return HTTPClientSingleton.getInstance().httpClient();
    }

    /**
     * The singleton HTTP client to send all requests.
     * The HTTP client manages HTTP connection in the BoundedAsynchronousPool automatically.
     * The protocol is plaintext HTTP 2.0.
     *
     * @return HTTP client singleton instance.
     */
    static SimpleHTTPClient plaintextHTTP2Client() {
        return PlaintextHTTP2ClientSingleton.getInstance().httpClient();
    }

    /**
     * The singleton HTTPs client to send all requests.
     * The HTTPs client manages HTTP connection in the BoundedAsynchronousPool automatically.
     * It uses ALPN to determine HTTP 1.1 or HTTP 2.0 protocol.
     *
     * @return HTTPs client singleton instance.
     */
    static SimpleHTTPClient httpsClient() {
        return HTTPsClientSingleton.getInstance().httpsClient();
    }

    /**
     * Create a new HTTP client instance.
     *
     * @return A new HTTP client instance.
     */
    static SimpleHTTPClient createHTTPClient() {
        return new SimpleHTTPClient();
    }

    /**
     * Create a new HTTP client instance.
     *
     * @param configuration HTTP client configuration.
     * @return A new HTTP client instance.
     */
    static SimpleHTTPClient createHTTPClient(SimpleHTTPClientConfiguration configuration) {
        return new SimpleHTTPClient(configuration);
    }

    /**
     * Create a new HTTPs client.
     *
     * @return A new HTTPs client.
     */
    static SimpleHTTPClient createHTTPsClient() {
        SimpleHTTPClientConfiguration configuration = new SimpleHTTPClientConfiguration();
        configuration.setSecureConnectionEnabled(true);
        return new SimpleHTTPClient(configuration);
    }

    /**
     * Create a new plaintext HTTP2 client.
     *
     * @return a new plaintext HTTP2 client.
     */
    static SimpleHTTPClient createPlaintextHTTP2Client() {
        SimpleHTTPClientConfiguration configuration = new SimpleHTTPClientConfiguration();
        configuration.setProtocol(HttpVersion.HTTP_2.asString());
        return new SimpleHTTPClient(configuration);
    }

    /**
     * Create a new HTTPs client.
     *
     * @param secureSessionFactory The secure session factory. We provide JDK or OpenSSL secure session factory.
     * @return A new HTTPs client.
     */
    static SimpleHTTPClient createHTTPsClient(SecureSessionFactory secureSessionFactory) {
        SimpleHTTPClientConfiguration configuration = new SimpleHTTPClientConfiguration();
        configuration.setSecureSessionFactory(secureSessionFactory);
        configuration.setSecureConnectionEnabled(true);
        return new SimpleHTTPClient(configuration);
    }

    /**
     * Use fluent API to create an new HTTP server instance.
     *
     * @return HTTP server builder.
     */
    static HTTP2ServerBuilder httpServer() {
        return new HTTP2ServerBuilder().httpServer();
    }

    /**
     * Create a new HTTP2 server. It uses the plaintext HTTP2 protocol.
     *
     * @return HTTP server builder.
     */
    static HTTP2ServerBuilder plaintextHTTP2Server() {
        SimpleHTTPServerConfiguration configuration = new SimpleHTTPServerConfiguration();
        configuration.setProtocol(HttpVersion.HTTP_2.asString());
        return httpServer(configuration);
    }

    /**
     * Create a new HTTP server.
     *
     * @param serverConfiguration The server configuration.
     * @return HTTP server builder
     */
    static HTTP2ServerBuilder httpServer(SimpleHTTPServerConfiguration serverConfiguration) {
        return httpServer(serverConfiguration, new HTTPBodyConfiguration());
    }

    /**
     * Create a new HTTP server.
     *
     * @param serverConfiguration   HTTP server configuration.
     * @param httpBodyConfiguration HTTP body process configuration.
     * @return HTTP server builder.
     */
    static HTTP2ServerBuilder httpServer(SimpleHTTPServerConfiguration serverConfiguration,
                                         HTTPBodyConfiguration httpBodyConfiguration) {
        return new HTTP2ServerBuilder().httpServer(serverConfiguration, httpBodyConfiguration);
    }

    /**
     * Create a new HTTPs server.
     *
     * @return HTTP server builder.
     */
    static HTTP2ServerBuilder httpsServer() {
        return new HTTP2ServerBuilder().httpsServer();
    }

    /**
     * Create a new HTTPs server.
     *
     * @param secureSessionFactory The secure session factory. We provide JDK or OpenSSL secure session factory.
     * @return HTTP server builder.
     */
    static HTTP2ServerBuilder httpsServer(SecureSessionFactory secureSessionFactory) {
        return new HTTP2ServerBuilder().httpsServer(secureSessionFactory);
    }

    /**
     * Create a new HTTP server instance
     *
     * @return A new HTTP server instance
     */
    static SimpleHTTPServer createHTTPServer() {
        return new SimpleHTTPServer();
    }

    /**
     * Create a new HTTP server instance
     *
     * @param configuration HTTP server configuration
     * @return A new HTTP server instance
     */
    static SimpleHTTPServer createHTTPServer(SimpleHTTPServerConfiguration configuration) {
        return new SimpleHTTPServer(configuration);
    }

    /**
     * Get the websocket client singleton.
     *
     * @return the websocket client singleton.
     */
    static SimpleWebSocketClient websocketClient() {
        return WebSocketClientSingleton.getInstance().webSocketClient();
    }

    /**
     * Create a new TCP client instance
     *
     * @return A new TCP client instance
     */
    static SimpleTcpClient createTCPClient() {
        return new SimpleTcpClient();
    }

    /**
     * Create a new TCP client instance
     *
     * @param configuration TCP client configuration
     * @return A new TCP client instance
     */
    static SimpleTcpClient createTCPClient(TcpConfiguration configuration) {
        return new SimpleTcpClient(configuration);
    }

    /**
     * Create a new TCP server instance
     *
     * @return a new TCP server instance
     */
    static SimpleTcpServer createTCPServer() {
        return new SimpleTcpServer();
    }

    /**
     * Create a new TCP server instance
     *
     * @param configuration TCP server configuration
     * @return a new TCP server instance
     */
    static SimpleTcpServer createTCPServer(TcpServerConfiguration configuration) {
        return new SimpleTcpServer(configuration);
    }

    /**
     * Create default application context. The default application context reads configuration firefly.xml in classpath.
     *
     * @return Default application context
     */
    static ApplicationContext createApplicationContext() {
        return new XmlApplicationContext();
    }

    /**
     * Create a new application context.
     *
     * @param path The configuration file path.
     * @return A new application context.
     */
    static ApplicationContext createApplicationContext(String path) {
        return new XmlApplicationContext(path);
    }

    /**
     * Get bean from default application context. The default application context reads configuration firefly.xml in classpath.
     *
     * @param clazz the bean's Class object
     * @param <T>   Bean type
     * @return A singleton bean instance by type
     */
    static <T> T getBean(Class<T> clazz) {
        return ApplicationContextSingleton.getInstance().getApplicationContext().getBean(clazz);
    }

    /**
     * Get the default application context.
     *
     * @return The default application context.
     */
    static ApplicationContext getApplicationContext() {
        return ApplicationContextSingleton.getInstance().getApplicationContext();
    }

    /**
     * Get bean from default application context. The default application context reads configuration firefly.xml in classpath.
     *
     * @param id  the bean's id
     * @param <T> bean type
     * @return A singleton bean instance by id
     */
    static <T> T getBean(String id) {
        return ApplicationContextSingleton.getInstance().getApplicationContext().getBean(id);
    }

    /**
     * Get all beans by type. The default application context reads configuration firefly.xml in classpath.
     *
     * @param clazz Bean's class object
     * @param <T>   Bean type
     * @return All beans are derived from type
     */
    static <T> Collection<T> getBeans(Class<T> clazz) {
        return ApplicationContextSingleton.getInstance().getApplicationContext().getBeans(clazz);
    }

    /**
     * Get all managed beans. The default application context reads configuration firefly.xml in classpath.
     *
     * @return The unmodifiable map of all beans
     */
    static Map<String, Object> getBeanMap() {
        return ApplicationContextSingleton.getInstance().getApplicationContext().getBeanMap();
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

        static void copy(InputStream in, OutputStream out) throws IOException {
            IO.copy(in, out);
        }

        static void copy(InputStream in, OutputStream out, long byteCount) throws IOException {
            IO.copy(in, out, byteCount);
        }

        static void copy(Reader in, Writer out) throws IOException {
            IO.copy(in, out);
        }

        static void copy(Reader in, Writer out, long byteCount) throws IOException {
            IO.copy(in, out, byteCount);
        }
    }

    interface buffer {
        static byte[] toArray(ByteBuffer buffer) {
            return BufferUtils.toArray(buffer);
        }

        static String toString(ByteBuffer buffer) {
            return BufferUtils.toUTF8String(buffer);
        }

        static String toString(ByteBuffer buffer, Charset charset) {
            return BufferUtils.toString(buffer, charset);
        }

        static int normalizeBufferSize(int capacity) {
            return BufferUtils.normalizeBufferSize(capacity);
        }

        static String toString(List<ByteBuffer> list) {
            return BufferUtils.toString(list);
        }

        static String toString(List<ByteBuffer> list, String charset) {
            return BufferUtils.toString(list, charset);
        }

        static List<ByteBuffer> split(ByteBuffer buffer, int maxSize) {
            return BufferUtils.split(buffer, maxSize);
        }

        static long remaining(Collection<ByteBuffer> collection) {
            return BufferUtils.remaining(collection);
        }
    }

    interface thread {
        static void sleep(long millis) {
            ThreadUtils.sleep(millis);
        }

        static void sleep(long sleepTime, TimeUnit timeUnit) {
            ThreadUtils.sleep(sleepTime, timeUnit);
        }
    }

    interface json {
        static String toJson(Object obj) {
            return Json.toJson(obj);
        }

        static <T> T parse(String json, Class<T> clazz) {
            return Json.toObject(json, clazz);
        }

        static <T> T parse(String json, GenericTypeReference<T> typeReference) {
            return Json.toObject(json, typeReference);
        }

        static <T> T parse(String json, Type type) {
            return Json.toObject(json, type);
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
        static StringBuilder newURIBuilder(String schema, String server, int port) {
            return URIUtils.newURIBuilder(schema, server, port);
        }

        static String newURI(String scheme, String server, int port, String path, String query) {
            return URIUtils.newURI(scheme, server, port, path, query);
        }

        static UrlEncoded encode() {
            return new UrlEncoded();
        }
    }

    interface javabean {
        static Map<String, PropertyAccess> getBeanAccess(GenericTypeReference genericTypeReference) {
            return BeanUtils.getBeanAccess(genericTypeReference);
        }

        static Map<String, PropertyAccess> getBeanAccess(Class<?> clazz) {
            return BeanUtils.getBeanAccess(clazz);
        }

        static void copyBean(Object src, Object dest) {
            BeanUtils.copyBean(src, dest);
        }
    }
}