package test.codec.http2.hpack;

import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.http2.hpack.HpackEncoder;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.io.BufferUtils;

public class HpackEncoderTest {
	@Test
	public void testUnknownFieldsContextManagement() {
		HpackEncoder encoder = new HpackEncoder(38 * 5);
		HttpFields fields = new HttpFields();

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

		// Add 4 entries
		for (int i = 0; i <= 3; i++)
			fields.add(field[i]);

		// encode them
		ByteBuffer buffer = BufferUtils.allocate(4096);
		int pos = BufferUtils.flipToFill(buffer);
		encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
		BufferUtils.flipToFlush(buffer, pos);

		// something was encoded!
		assertThat(buffer.remaining(), Matchers.greaterThan(0));

		// All are in the dynamic table
		Assert.assertEquals(4, encoder.getHpackContext().size());

		// encode exact same fields again!
		BufferUtils.clearToFill(buffer);
		encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
		BufferUtils.flipToFlush(buffer, 0);

		// All are in the dynamic table
		Assert.assertEquals(4, encoder.getHpackContext().size());

		// Add 4 more fields
		for (int i = 4; i <= 7; i++)
			fields.add(field[i]);

		// encode
		BufferUtils.clearToFill(buffer);
		encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
		BufferUtils.flipToFlush(buffer, 0);

		// something was encoded!
		assertThat(buffer.remaining(), Matchers.greaterThan(0));

		// max dynamic table size reached
		Assert.assertEquals(5, encoder.getHpackContext().size());

		// remove some fields
		for (int i = 0; i <= 7; i += 2)
			fields.remove(field[i].getName());

		// encode
		BufferUtils.clearToFill(buffer);
		encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
		BufferUtils.flipToFlush(buffer, 0);

		// something was encoded!
		assertThat(buffer.remaining(), Matchers.greaterThan(0));

		// max dynamic table size reached
		Assert.assertEquals(5, encoder.getHpackContext().size());

		// remove another fields
		fields.remove(field[1].getName());

		// encode
		BufferUtils.clearToFill(buffer);
		encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
		BufferUtils.flipToFlush(buffer, 0);

		// something was encoded!
		assertThat(buffer.remaining(), Matchers.greaterThan(0));

		// max dynamic table size reached
		Assert.assertEquals(5, encoder.getHpackContext().size());

		// re add the field

		fields.add(field[1]);

		// encode
		BufferUtils.clearToFill(buffer);
		encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
		BufferUtils.flipToFlush(buffer, 0);

		// something was encoded!
		assertThat(buffer.remaining(), Matchers.greaterThan(0));

		// max dynamic table size reached
		Assert.assertEquals(5, encoder.getHpackContext().size());

	}

	@Test
	public void testNeverIndexSetCookie() {
		HpackEncoder encoder = new HpackEncoder(38 * 5);
		ByteBuffer buffer = BufferUtils.allocate(4096);

		HttpFields fields = new HttpFields();
		fields.put("set-cookie", "some cookie value");

		// encode
		BufferUtils.clearToFill(buffer);
		encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
		BufferUtils.flipToFlush(buffer, 0);

		// something was encoded!
		assertThat(buffer.remaining(), Matchers.greaterThan(0));

		// empty dynamic table
		Assert.assertEquals(0, encoder.getHpackContext().size());

		// encode again
		BufferUtils.clearToFill(buffer);
		encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
		BufferUtils.flipToFlush(buffer, 0);

		// something was encoded!
		assertThat(buffer.remaining(), Matchers.greaterThan(0));

		// empty dynamic table
		Assert.assertEquals(0, encoder.getHpackContext().size());

	}

}
