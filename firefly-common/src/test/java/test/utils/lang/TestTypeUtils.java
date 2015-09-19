package test.utils.lang;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.lang.TypeUtils;

public class TestTypeUtils {
	@Test
	public void convertHexDigitTest() {
		Assert.assertEquals((byte) 0, TypeUtils.convertHexDigit((byte) '0'));
		Assert.assertEquals((byte) 9, TypeUtils.convertHexDigit((byte) '9'));
		Assert.assertEquals((byte) 10, TypeUtils.convertHexDigit((byte) 'a'));
		Assert.assertEquals((byte) 10, TypeUtils.convertHexDigit((byte) 'A'));
		Assert.assertEquals((byte) 15, TypeUtils.convertHexDigit((byte) 'f'));
		Assert.assertEquals((byte) 15, TypeUtils.convertHexDigit((byte) 'F'));

		Assert.assertEquals((int) 0, TypeUtils.convertHexDigit((int) '0'));
		Assert.assertEquals((int) 9, TypeUtils.convertHexDigit((int) '9'));
		Assert.assertEquals((int) 10, TypeUtils.convertHexDigit((int) 'a'));
		Assert.assertEquals((int) 10, TypeUtils.convertHexDigit((int) 'A'));
		Assert.assertEquals((int) 15, TypeUtils.convertHexDigit((int) 'f'));
		Assert.assertEquals((int) 15, TypeUtils.convertHexDigit((int) 'F'));
	}

	@Test
	public void testToHexInt() throws Exception {
		StringBuilder b = new StringBuilder();

		b.setLength(0);
		TypeUtils.toHex((int) 0, b);
		Assert.assertEquals("00000000", b.toString());

		b.setLength(0);
		TypeUtils.toHex(Integer.MAX_VALUE, b);
		Assert.assertEquals("7FFFFFFF", b.toString());

		b.setLength(0);
		TypeUtils.toHex(Integer.MIN_VALUE, b);
		Assert.assertEquals("80000000", b.toString());

		b.setLength(0);
		TypeUtils.toHex(0x12345678, b);
		Assert.assertEquals("12345678", b.toString());

		b.setLength(0);
		TypeUtils.toHex(0x9abcdef0, b);
		Assert.assertEquals("9ABCDEF0", b.toString());
	}

	@Test
	public void testToHexLong() throws Exception {
		StringBuilder b = new StringBuilder();

		b.setLength(0);
		TypeUtils.toHex((long) 0, b);
		Assert.assertEquals("0000000000000000", b.toString());

		b.setLength(0);
		TypeUtils.toHex(Long.MAX_VALUE, b);
		Assert.assertEquals("7FFFFFFFFFFFFFFF", b.toString());

		b.setLength(0);
		TypeUtils.toHex(Long.MIN_VALUE, b);
		Assert.assertEquals("8000000000000000", b.toString());

		b.setLength(0);
		TypeUtils.toHex(0x123456789abcdef0L, b);
		Assert.assertEquals("123456789ABCDEF0", b.toString());
	}

	@Test
	public void testIsTrue() throws Exception {
		Assert.assertTrue(TypeUtils.isTrue(Boolean.TRUE));
		Assert.assertTrue(TypeUtils.isTrue(true));
		Assert.assertTrue(TypeUtils.isTrue("true"));
		Assert.assertTrue(TypeUtils.isTrue(new Object() {
			@Override
			public String toString() {
				return "true";
			}
		}));

		Assert.assertFalse(TypeUtils.isTrue(Boolean.FALSE));
		Assert.assertFalse(TypeUtils.isTrue(false));
		Assert.assertFalse(TypeUtils.isTrue("false"));
		Assert.assertFalse(TypeUtils.isTrue("blargle"));
		Assert.assertFalse(TypeUtils.isTrue(new Object() {
			@Override
			public String toString() {
				return "false";
			}
		}));
	}

	@Test
	public void testIsFalse() throws Exception {
		Assert.assertTrue(TypeUtils.isFalse(Boolean.FALSE));
		Assert.assertTrue(TypeUtils.isFalse(false));
		Assert.assertTrue(TypeUtils.isFalse("false"));
		Assert.assertTrue(TypeUtils.isFalse(new Object() {
			@Override
			public String toString() {
				return "false";
			}
		}));

		Assert.assertFalse(TypeUtils.isFalse(Boolean.TRUE));
		Assert.assertFalse(TypeUtils.isFalse(true));
		Assert.assertFalse(TypeUtils.isFalse("true"));
		Assert.assertFalse(TypeUtils.isFalse("blargle"));
		Assert.assertFalse(TypeUtils.isFalse(new Object() {
			@Override
			public String toString() {
				return "true";
			}
		}));
	}
}
