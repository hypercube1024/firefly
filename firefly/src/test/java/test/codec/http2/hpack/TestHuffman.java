package test.codec.http2.hpack;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.http2.hpack.Huffman;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.lang.TypeUtils;

public class TestHuffman
{
    String[][] tests =
        {
            {"D.4.1","f1e3c2e5f23a6ba0ab90f4ff","www.example.com"},
            {"D.4.2","a8eb10649cbf","no-cache"},
            {"D.6.1k","6402","302"},
            {"D.6.1v","aec3771a4b","private"},
            {"D.6.1d","d07abe941054d444a8200595040b8166e082a62d1bff","Mon, 21 Oct 2013 20:13:21 GMT"},
            {"D.6.1l","9d29ad171863c78f0b97c8e9ae82ae43d3","https://www.example.com"},
            {"D.6.2te","640cff","303"},
        };
    
    @Test
    public void testDecode() throws Exception
    {
        for (String[] test:tests)
        {
            byte[] encoded=TypeUtils.fromHexString(test[1]);
            String decoded=Huffman.decode(ByteBuffer.wrap(encoded));
            Assert.assertEquals(test[0],test[2],decoded);
        }
    }
    
    @Test
    public void testDecodeTrailingFF() throws Exception
    {
        for (String[] test:tests)
        {
            byte[] encoded=TypeUtils.fromHexString(test[1]+"FF");
            String decoded=Huffman.decode(ByteBuffer.wrap(encoded));
            Assert.assertEquals(test[0],test[2],decoded);
        }
    }
    
    @Test
    public void testEncode() throws Exception
    {
        for (String[] test:tests)
        {
            ByteBuffer buf = BufferUtils.allocate(1024);
            int pos=BufferUtils.flipToFill(buf);
            Huffman.encode(buf,test[2]);
            BufferUtils.flipToFlush(buf,pos);
            String encoded=TypeUtils.toHexString(BufferUtils.toArray(buf)).toLowerCase();
            Assert.assertEquals(test[0],test[1],encoded);
            Assert.assertEquals(test[1].length()/2,Huffman.octetsNeeded(test[2]));
        }
    }

    @Test
    public void testEncode8859Only() throws Exception
    {
        char bad[] = {(char)128,(char)0,(char)-1,' '-1};
        for (int i=0;i<bad.length;i++)
        {
            String s="bad '"+bad[i]+"'";
            
            try
            {
                Huffman.octetsNeeded(s);
                Assert.fail("i="+i);
            }
            catch(IllegalArgumentException e)
            {
            }
            
            try
            {
                Huffman.encode(BufferUtils.allocate(32),s);
                Assert.fail("i="+i);
            }
            catch(IllegalArgumentException e)
            {
            }
        }
    }
    
    
}
