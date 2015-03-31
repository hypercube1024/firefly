package test.codec.spdy.compression;

import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.spdy.frames.compression.CompressionDictionary;
import com.firefly.codec.spdy.frames.compression.CompressionFactory;
import com.firefly.codec.spdy.frames.compression.DefaultCompressionFactory;

public class TestDefaultCompressionFactory {
	
	static final CompressionFactory compressionFactory = DefaultCompressionFactory.getCompressionfactory();
	static final CompressionFactory.Compressor compressor = compressionFactory.newCompressor();
	static final CompressionFactory.Decompressor decompressor = compressionFactory.newDecompressor();
	static {
		compressor.setDictionary(CompressionDictionary.DICTIONARY_V3);
		decompressor.setDefaultDictionary(CompressionDictionary.DICTIONARY_V3);
	}
	
	@Test
	public void test1() throws Throwable{
		String str = "get /hello/world http/1.1";
		
		byte[] src = str.getBytes("UTF-8");
		System.out.println("src length is " + src.length);
		ByteBuffer b = compressor.compressToByteBuffer(src);
		System.out.println("compressed length is " + b.remaining());
		
		byte[] compressed = new byte[b.remaining()];
		b.get(compressed);
		
		ByteBuffer b2 = decompressor.decompressToByteBuffer(compressed);
		System.out.println("decompressed length is " + b2.remaining());
		byte[] decompressed = new byte[b2.remaining()];
		b2.get(decompressed);
		System.out.println(new String(decompressed, "UTF-8"));
		
		Assert.assertThat(new String(decompressed, "UTF-8"), is(str));
	}
	
	@Test
	public void test2() throws Throwable{
		String str = "get /hello/world http/1.1";
		for (int i = 0; i < 20; i++) {
			str += "post /hello/world" + i + " http/1.1 "; 
		}
		
		byte[] src = str.getBytes("UTF-8");
		System.out.println("src length is " + src.length);
		ByteBuffer b = compressor.compressToByteBuffer(src);
		System.out.println("compressed length is " + b.remaining());
		
		byte[] compressed = new byte[b.remaining()];
		b.get(compressed);
		
		ByteBuffer b2 = decompressor.decompressToByteBuffer(compressed);
		System.out.println("decompressed length is " + b2.remaining());
		byte[] decompressed = new byte[b2.remaining()];
		b2.get(decompressed);
		System.out.println(new String(decompressed, "UTF-8"));
		
		Assert.assertThat(new String(decompressed, "UTF-8"), is(str));
	}
}
