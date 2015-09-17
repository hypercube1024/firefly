package test.codec.http2.hpack;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.http2.hpack.HpackContext;
import com.firefly.codec.http2.hpack.HpackContext.Entry;
import com.firefly.codec.http2.model.HttpField;

public class TestHpackContext {
	@Test
    public void testStaticName() {
        HpackContext ctx = new HpackContext(4096);
        Entry entry = ctx.get(":method");
        Assert.assertThat(entry.getHttpField().getName(), is(":method"));
        Assert.assertTrue(entry.isStatic());
        Assert.assertThat(entry.toString(),Matchers.startsWith("{S,2,:method: "));
    }
	
	@Test
    public void testEmptyAdd() {
        HpackContext ctx = new HpackContext(0);
        HttpField field = new HttpField("foo","bar");
        assertNull(ctx.add(field));
    }
	
	@Test
    public void testTooBigAdd() {
        HpackContext ctx = new HpackContext(37);
        HttpField field = new HttpField("foo","bar");
        assertNull(ctx.add(field));
    }
	
	@Test
    public void testJustRight() {
        HpackContext ctx = new HpackContext(38);
        HttpField field = new HttpField("foo","bar");
        Entry entry=ctx.add(field);
        Assert.assertNotNull(entry);
        Assert.assertThat(entry.toString(),Matchers.startsWith("{D,0,foo: bar,"));
    }
	
	@Test
    public void testEvictOne() {
        HpackContext ctx = new HpackContext(38);
        HttpField field0 = new HttpField("foo","bar");
        
        assertEquals(field0,ctx.add(field0).getHttpField());
        assertEquals(field0,ctx.get("foo").getHttpField());
        
        HttpField field1 = new HttpField("xxx","yyy");
        assertEquals(field1,ctx.add(field1).getHttpField());

        assertNull(ctx.get(field0));
        assertNull(ctx.get("foo"));
        assertEquals(field1,ctx.get(field1).getHttpField());
        assertEquals(field1,ctx.get("xxx").getHttpField());
    }
	
	@Test
	public void testEvictNames() {
	    HpackContext ctx = new HpackContext(38*2);
	    HttpField[] field = {
	       new HttpField("name","v0"),
	       new HttpField("name","v1"),
	       new HttpField("name","v2"),
	       new HttpField("name","v3"),
	       new HttpField("name","v4"),
	       new HttpField("name","v5"),
	    };
	    
	    Entry[] entry = new Entry[field.length];
	    
	    // Add 2 name entries to fill table
	    for (int i=0;i<=1;i++)
	        entry[i]=ctx.add(field[i]);
	    
	    // check there is a name reference and it is the most recent added
	    assertEquals(entry[1],ctx.get("name"));
	
	    // Add 1 other entry to table and evict 1
	    ctx.add(new HttpField("xxx","yyy"));
	    
	    // check the name reference has been not been evicted
	    assertEquals(entry[1],ctx.get("name"));
	    
	    // Add 1 other entry to table and evict 1
	    ctx.add(new HttpField("foo","bar"));
	    
	    // name is evicted
	    assertNull(ctx.get("name"));
	}
	
	@Test
    public void testGetAddStatic() {
        HpackContext ctx = new HpackContext(4096);

        // Look for the field.  Should find static version.
        HttpField methodGet = new HttpField(":method","GET");
        assertEquals(methodGet,ctx.get(methodGet).getHttpField());
        assertTrue(ctx.get(methodGet).isStatic());
        
        // Add static version to dynamic table
        Entry e0 = ctx.add(ctx.get(methodGet).getHttpField());
        
        // Look again and should see dynamic version
        assertEquals(methodGet,ctx.get(methodGet).getHttpField());
        assertFalse(methodGet == ctx.get(methodGet).getHttpField());
        assertFalse(ctx.get(methodGet).isStatic());
        
        // Duplicates allows
        Entry e1 = ctx.add(ctx.get(methodGet).getHttpField());
        
        // Look again and should see dynamic version
        assertEquals(methodGet,ctx.get(methodGet).getHttpField());
        assertFalse(methodGet == ctx.get(methodGet).getHttpField());
        assertFalse(ctx.get(methodGet).isStatic());
        assertFalse(e0 == e1);
    }
	
	@Test
    public void testGetAddStaticName() {
        HpackContext ctx = new HpackContext(4096);
        HttpField methodOther = new HttpField(":method","OTHER");

        // Look for the field by name.  Should find static version.
        assertEquals(":method", ctx.get(":method").getHttpField().getName());
        assertTrue(ctx.get(":method").isStatic());
        
        // Add dynamic entry with method
        ctx.add(methodOther);
        
        // Look for the field by name.  Should find static version.
        assertEquals(":method", ctx.get(":method").getHttpField().getName());
        assertTrue(ctx.get(":method").isStatic()); 
    }
	
	@Test
    public void testIndexes() {
		// Only enough space for 5 entries
        HpackContext ctx = new HpackContext(38 * 5);
        
        HttpField methodPost = new HttpField(":method", "POST");
        HttpField[] field = 
        {
           new HttpField("fo0","b0r"),
           new HttpField("fo1","b1r"),
           new HttpField("fo2","b2r"),
           new HttpField("fo3","b3r"),
           new HttpField("fo4","b4r"),
           new HttpField("fo5","b5r"),
           new HttpField("fo6","b6r"),
           new HttpField("fo7","b7r"),
           new HttpField("fo8","b8r"),
           new HttpField("fo9","b9r"),
           new HttpField("foA","bAr"),
        };
        
        Entry[] entry = new Entry[100];
        
        // Lookup the index of a static field
        assertEquals(0, ctx.size());
        assertEquals(":authority", ctx.get(1).getHttpField().getName());
        assertEquals(3, ctx.index(ctx.get(methodPost)));
        assertEquals(methodPost, ctx.get(3).getHttpField());
        assertEquals("www-authenticate", ctx.get(61).getHttpField().getName());
        assertEquals(null, ctx.get(62));
        
        // Add a single entry  
        entry[0] = ctx.add(field[0]);
        
        // Check new entry is 62 
        assertEquals(1, ctx.size());
        assertEquals(62, ctx.index(entry[0]));
        assertEquals(entry[0], ctx.get(62));
        
        // and statics have moved up 0
        assertEquals(":authority", ctx.get(1).getHttpField().getName());
        assertEquals(3, ctx.index(ctx.get(methodPost)));
        assertEquals(methodPost, ctx.get(3).getHttpField());
        assertEquals("www-authenticate", ctx.get(61).getHttpField().getName());
        assertEquals(null, ctx.get(62 + ctx.size()));
    }
}
