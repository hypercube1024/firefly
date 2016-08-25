package test.codec.http2;

import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.util.concurrent.ExecutionException;

import com.firefly.client.http2.ClientHTTPHandler;
import com.firefly.client.http2.HTTP2Client;
import com.firefly.client.http2.HTTPClientConnection;
import com.firefly.codec.http2.model.HostPortHttpField;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpScheme;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.io.BufferUtils;

public class HTTP2ClientTLSDemo2 {

	// private static Log log =
	// LogFactory.getInstance().getLog("firefly-system");

	public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.getTcpConfiguration().setTimeout(60 * 1000);
		http2Configuration.setSecureConnectionEnabled(true);
		HTTP2Client client = new HTTP2Client(http2Configuration);

		FuturePromise<HTTPClientConnection> promise = new FuturePromise<>();
		client.connect("127.0.0.1", 6655, promise);

		final HTTPClientConnection httpConnection = promise.get();

		// ClientHTTPHandler handler = new ClientHTTPHandler.Adapter() {
		//
		// FileChannel fc = FileChannel.open(Paths.get("D:/favicon.ico"),
		// StandardOpenOption.WRITE,
		// StandardOpenOption.CREATE);
		//
		// @Override
		// public boolean content(ByteBuffer item, Request request, Response
		// response, HTTPOutputStream output,
		// HTTPConnection connection) {
		// try {
		// fc.write(item);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// return false;
		// }
		//
		// @Override
		// public boolean messageComplete(Request request, Response response,
		// HTTPOutputStream output,
		// HTTPConnection connection) {
		// log.info("client received frame: {}, {}, {}", response.getStatus(),
		// response.getReason(),
		// response.getFields());
		// try {
		// fc.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// return true;
		// }
		// };

		HttpFields fields = new HttpFields();
		fields.put(HttpHeader.USER_AGENT, "Firefly Client 1.0");

		if (httpConnection.getHttpVersion() == HttpVersion.HTTP_2) {
			// httpConnection.send(new MetaData.Request("GET", HttpScheme.HTTP,
			// new HostPortHttpField("127.0.0.1:6655"),
			// "/favicon.ico", HttpVersion.HTTP_1_1, fields), handler);

			httpConnection.send(
					new MetaData.Request("GET", HttpScheme.HTTP, new HostPortHttpField("127.0.0.1:6655"), "/index",
							HttpVersion.HTTP_1_1, fields),
					new ClientHTTPHandler.Adapter().messageComplete((req, resp, outputStream, conn) -> {
						System.out.println("message complete: " + resp.getStatus() + "|" + resp.getReason());
						return true;
					}).content((buffer, req, resp, outputStream, conn) -> {
						System.out.println(BufferUtils.toString(buffer, StandardCharsets.UTF_8));
						return false;
					}).badMessage((errCode, reason, req, resp, outputStream, conn) -> {
						System.out.println("error: " + errCode + "|" + reason);
					}));
		}
	}

}
