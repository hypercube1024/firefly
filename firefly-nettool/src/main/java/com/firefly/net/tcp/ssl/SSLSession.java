package com.firefly.net.tcp.ssl;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class SSLSession implements Closeable {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private Session session;
	private SSLEngine sslEngine;
	
	private int appBufferSize;
	
	private ByteBuffer inNetBuffer;
    
    /*
     * All of the inbound request data lives here until we determine
     * that we've read everything, then we pass that data back to the
     * caller.
     */
    protected ByteBuffer requestBuffer;
    private static final int requestBufferSize = 1024 * 8;
    private static final int writeBufferSize = 1024 * 8;
    
    /*
     * An empty ByteBuffer for use when one isn't available, say
     * as a source buffer during initial handshake wraps or for close
     * operations.
     */
    private static final ByteBuffer hsBuffer = ByteBuffer.allocate(0);

    /*
     * During our initial handshake, keep track of the next
     * SSLEngine operation that needs to occur:
     *
     *     NEED_WRAP/NEED_UNWRAP
     *
     * Once the initial handshake has completed, we can short circuit
     * handshake checks with initialHSComplete.
     */
    private HandshakeStatus initialHSStatus;
    private boolean initialHSComplete;

    /*
     * We have received the shutdown request by our caller, and have
     * closed our outbound side.
     */
    private boolean closed = false;
    
    public SSLSession(SSLContext sslContext, Session session) throws Throwable {
    	this.session = session;
    	requestBuffer = ByteBuffer.allocate(requestBufferSize);
    	
        sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(false);
        initialHSStatus = HandshakeStatus.NEED_UNWRAP;
        initialHSComplete = false;
        
        /*
         * Create a buffer using the normal expected application size we'll
         * be getting.  This may change, depending on the peer's
         * SSL implementation.
         */
        appBufferSize = sslEngine.getSession().getApplicationBufferSize();
    }
    
    /**
     * The initial handshake is a procedure by which the two peers exchange communication parameters until an SSLSession is established. 
     * Application data can not be sent during this phase.
     * @param receiveBuffer encrypted message
     * @return It return true means handshake success
     * @throws Throwable
     */
    public boolean doHandshake(ByteBuffer receiveBuffer) throws Throwable {
    	if(!session.isOpen()) {
    		close();
        	return (initialHSComplete = false);
        }

        if (initialHSComplete)
            return initialHSComplete;
        
        if(initialHSStatus == HandshakeStatus.FINISHED) {
        	log.info("session {} handshake end", session.getSessionId());
        	return (initialHSComplete = true);
        }
        
        switch (initialHSStatus) {
        case NEED_UNWRAP:
        	doHandshakeReceive(receiveBuffer);
            if (initialHSStatus != HandshakeStatus.NEED_WRAP)
                break;

        case NEED_WRAP:
            doHandshakeResponse();
            break;

        default: // NOT_HANDSHAKING/NEED_TASK/FINISHED
            throw new RuntimeException("Invalid Handshaking State" + initialHSStatus);
        }
        return initialHSComplete;
    }
    
    private void copyNetBuffer(ByteBuffer now) {
    	if(inNetBuffer == null) {
    		inNetBuffer = now;
    		return;
    	}
    	
    	inNetBuffer.flip();
    	ByteBuffer bb = ByteBuffer.allocate(inNetBuffer.remaining() + now.remaining());
    	bb.put(inNetBuffer).put(now).flip();
    	inNetBuffer = bb;
    }
    
    private void doHandshakeReceive(ByteBuffer receiveBuffer) throws Throwable {
    	SSLEngineResult result;
    	
    	if(receiveBuffer != null)
    		copyNetBuffer(receiveBuffer);
    	
    	needIO:
        while (initialHSStatus == HandshakeStatus.NEED_UNWRAP) {
            resizeRequestBuffer();    // expected room for unwrap
            result = sslEngine.unwrap(inNetBuffer, requestBuffer);
            if(!inNetBuffer.hasRemaining())
            	inNetBuffer = null;
            
            initialHSStatus = result.getHandshakeStatus();

            switch (result.getStatus()) {
            case OK:
                switch (initialHSStatus) {
                case NOT_HANDSHAKING:
                    throw new IOException("Not handshaking during initial handshake");

                case NEED_TASK:
                    initialHSStatus = doTasks();
                    break;

                case FINISHED:
                    initialHSComplete = true;
                    log.info("session {} handshake end", session.getSessionId());
                    break needIO;
				default:
					break;
                }
                break;

            case BUFFER_UNDERFLOW:
                break needIO;

            case BUFFER_OVERFLOW:
                // Reset the application buffer size.
                appBufferSize = sslEngine.getSession().getApplicationBufferSize();
                break;

            default: //CLOSED:
                throw new IOException("Received" + result.getStatus() + "during initial handshaking");
            }
        }  // "needIO" block.
    }
    
    private void doHandshakeResponse() throws Throwable {
    	while(initialHSStatus == HandshakeStatus.NEED_WRAP) {
	    	SSLEngineResult result;
	    	ByteBuffer writeBuf = ByteBuffer.allocate(writeBufferSize);
	    	
	    	wrap:
	    	while(true) {
		        result = sslEngine.wrap(hsBuffer, writeBuf);
		
		        initialHSStatus = result.getHandshakeStatus();
		
		        switch (result.getStatus()) {
		        case OK:
		            if (initialHSStatus == HandshakeStatus.NEED_TASK)
		                initialHSStatus = doTasks();
		
		            writeBuf.flip();
		            session.write(writeBuf);
		            break wrap;
		            
		        case BUFFER_OVERFLOW:
		            int netSize = sslEngine.getSession().getPacketBufferSize();
		            ByteBuffer b = ByteBuffer.allocate(writeBuf.position() + netSize);
		            writeBuf.flip();
		            b.put(writeBuf);
		            writeBuf = b;
		            // retry the operation.
		        	break;
		
		        default: // BUFFER_OVERFLOW/BUFFER_UNDERFLOW/CLOSED:
		            throw new IOException("Received" + result.getStatus() + "during initial handshaking");
		        }
	    	}
    	}
    }
    
    /**
     * This method is used to decrypt, it implied do handshake
     * @param receiveBuffer encrypted message
     * @return plaintext
     * @throws Throwable sslEngine error during data read
     */
    public ByteBuffer read(ByteBuffer receiveBuffer) throws Throwable {
    	if(!doHandshake(receiveBuffer))
			return null;
        
        copyNetBuffer(receiveBuffer);
        SSLEngineResult result;

        while(true) {
        	resizeRequestBuffer();    // expected room for unwrap
            result = sslEngine.unwrap(inNetBuffer, requestBuffer);
            if(!inNetBuffer.hasRemaining())
            	inNetBuffer = null;

            /*
             * Could check here for a renegotation, but we're only
             * doing a simple read/write, and won't have enough state
             * transitions to do a complete handshake, so ignore that
             * possibility.
             */
            switch (result.getStatus()) {

            case BUFFER_OVERFLOW:
                appBufferSize = sslEngine.getSession().getApplicationBufferSize();
                break;

            case BUFFER_UNDERFLOW:
                return null;
                
            case OK:
                if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                    doTasks();
                }
                return getRequestBuffer();

            default:
                throw new IOException("sslEngine error during data read: " + result.getStatus());
            }
        }
    }
    
    /**
     * This method is used to encrypt and flush to socket channel
     * @param outputBuffer plaintext message
     * @return writen length
     * @throws Throwable sslEngine error during data write
     */
    public int write(ByteBuffer outputBuffer) throws Throwable {
    	if (!initialHSComplete)
            throw new IllegalStateException();
    	
    	int ret = 0;
    	if(!outputBuffer.hasRemaining())
    		return ret;
    	
    	final int remain = outputBuffer.remaining();
//    	log.info("src remain {}", remain);
    	
    	while(ret < remain) {
    		ByteBuffer writeBuf = ByteBuffer.allocate(writeBufferSize);
    		
    		wrap:
    		while(true) {
		    	SSLEngineResult result = sslEngine.wrap(outputBuffer, writeBuf);
		        ret += result.bytesConsumed();
//		        log.info("consumed data: {} | {}", ret, outputBuffer.remaining());
		        
		        switch (result.getStatus()) {
		        case OK:
		            if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK)
		                doTasks();
		            
		            writeBuf.flip();
		            session.write(writeBuf);
		            break wrap;
		
		        case BUFFER_OVERFLOW:
		            int netSize = sslEngine.getSession().getPacketBufferSize();
//		            log.info("sssss: {}", (writeBuf.position() + netSize));
		            ByteBuffer b = ByteBuffer.allocate(writeBuf.position() + netSize);
		            writeBuf.flip();
		            b.put(writeBuf);
		            writeBuf = b;
		            // retry the operation.
		        	break;
		        default:
		            throw new IOException("sslEngine error during data write: " + result.getStatus());
		        }
    		}
    	}
    	
    	return ret;
    }
    
    /**
     * Perform a FileChannel.TransferTo on the socket channel.
     * <P>
     * We have to copy the data into an intermediary app ByteBuffer
     * first, then send it through the SSLEngine.
     * <P>
     * We return the number of bytes actually read out of the
     * filechannel.  However, the data may actually be stuck
     * in the fileChannelBB or the outNetBB.  The caller
     * is responsible for making sure to call dataFlush()
     * before shutting down.
     * 
     * @param fc to transfer FileChannel
     * @param pos start position
     * @param len length
     * @return writen length
     * @throws Throwable
     */
    public long transferTo(FileChannel fc, long pos, long len) throws Throwable {
    	if (!initialHSComplete)
            throw new IllegalStateException();
    	
    	long ret = 0;
    	try {
	    	ByteBuffer buf = ByteBuffer.allocate(1024 * 4);
	    	for (int i = 0; (i = fc.read(buf, pos)) != -1;) {
				ret += i;
				buf.flip();
				write(buf);
				
				if(ret >= len)
					break;
			}
    	} finally {
    		fc.close();
    	}
    	return ret;
    }
    
    /**
     * Calls up to the superclass to adjust the buffer size
     * by an appropriate increment.
     */
    protected void resizeRequestBuffer() {
        resizeRequestBuffer(appBufferSize);
    }
    
    /**
     * Return a ByteBuffer with "remaining" space to work.  If you have to
     * reallocate the ByteBuffer, copy the existing info into the new buffer.
     */
    protected void resizeRequestBuffer(int remaining) {
        if (requestBuffer.remaining() < remaining) {
            ByteBuffer bb = ByteBuffer.allocate(requestBuffer.capacity() * 2);
            requestBuffer.flip();
            bb.put(requestBuffer);
            requestBuffer = bb;
        }
    }
    
    protected ByteBuffer getRequestBuffer() {
    	requestBuffer.flip();
    	ByteBuffer buf = ByteBuffer.allocate(requestBuffer.remaining());
    	buf.put(requestBuffer).flip();
    	requestBuffer.flip();
	    return buf;
    }
    
//    private ByteBuffer getWriteBuffer(int remain) {
//    	int netBufferSize = sslEngine.getSession().getPacketBufferSize();
//    	return remain >= netBufferSize ? ByteBuffer.allocate(netBufferSize) : ByteBuffer.allocate(remain);
//    }
    
    /**
     * Do all the outstanding handshake tasks in the current Thread.
     */
    protected SSLEngineResult.HandshakeStatus doTasks() {
        Runnable runnable;
        
        // We could run this in a separate thread, but do in the current for now.
        while ((runnable = sslEngine.getDelegatedTask()) != null) {
            runnable.run();
        }
        return sslEngine.getHandshakeStatus();
    }

	@Override
	public void close() throws IOException {
		if (!closed) {
			sslEngine.closeInbound();
            sslEngine.closeOutbound();
            closed = true;
        }
	} 
    
}
