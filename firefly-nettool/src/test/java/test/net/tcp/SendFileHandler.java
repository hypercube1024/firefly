package test.net.tcp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;

import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class SendFileHandler implements Handler {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	@Override
	public void sessionOpened(Session session) throws Throwable {
		log.debug("session open |" + session.getSessionId());
		log.debug("local: " + session.getLocalAddress());
		log.debug("remote: " + session.getRemoteAddress());
	}

	@Override
	public void sessionClosed(Session session) throws Throwable {
		log.debug("session close|" + session.getSessionId());
	}

	@Override
	public void messageRecieved(Session session, Object message) throws Throwable {
		String str = (String) message;
		if (str.equals("quit")) {
			session.encode("bye!");
			session.close(false);
		} else if (str.equals("getfile")) {
			RandomAccessFile raf = null;
			File file = null;
			try {
				file = new File(SendFileHandler.class.getResource(
						"/testFile.txt").toURI());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			try {
				raf = new RandomAccessFile(file, "r");
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
			}
			FileRegion fileRegion = null;
			try {
                assert raf != null;
                fileRegion = new FileRegion(raf.getChannel(), 0, raf.length());
			} catch (IOException e) {
				e.printStackTrace();
			}
			session.write(fileRegion);
		} else {
			log.debug("recive: " + str);
			session.encode(message);
		}
		log.debug("r {}  {} | w {} {}", session.getReadBytes(), str, session.getWrittenBytes(), message);
	}

	@Override
	public void exceptionCaught(Session session, Throwable t) throws Throwable{
		log.error(t.getMessage() + "|" + session.getSessionId(), t);
	}

}
