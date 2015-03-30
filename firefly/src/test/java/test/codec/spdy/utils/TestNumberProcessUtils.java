package test.codec.spdy.utils;

import static org.hamcrest.Matchers.is;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.spdy.decode.utils.NumberProcessUtils;

public class TestNumberProcessUtils {
	
	@Test
	public void testToUnsigned24bitsInteger() {
		int i = 0b00000000_11101111_00011100_11101010;
		byte a1 = (byte)0b11101111;
		short a2 = (short)0b00011100_11101010;
		Assert.assertThat(NumberProcessUtils.toUnsigned24bitsInteger(a1, a2), is(i));
	}
	
	@Test
	public void testToUnsignedInteger() {
		byte i = (byte)0b11000000;
		System.out.println("byte: " + i);
		int x = 0b11000000;
		System.out.println("integer: " + x);
		Assert.assertThat(NumberProcessUtils.toUnsignedInteger(i), is(x));
		
		i = (byte)0b01000011;
		x = 0b01000011;
		Assert.assertThat(NumberProcessUtils.toUnsignedInteger(i), is(x));
		
		short s = (short)0b11000000_11101010;
		System.out.println("short: " + i);
		x = 0b11000000_11101010;
		System.out.println("integer: " + x);
		Assert.assertThat(NumberProcessUtils.toUnsignedInteger(s), is(x));
		
		s = (short)0b01000000_11101010;
		x = 0b01000000_11101010;
		Assert.assertThat(NumberProcessUtils.toUnsignedInteger(s), is(x));
	}
	
	@Test
	public void testToUnsignedLong() {
		int i = 0b11000000_11101010_11110000_10100011;
		System.out.println("3 integer: " + i);
		long j = 0b11000000_11101010_11110000_10100011L;
		System.out.println("3 long: " + j);
		Assert.assertThat(NumberProcessUtils.toUnsignedLong(i), is(j));
	}
	
	@Test
	public void testToUnsigned15bitsShort() {
		short s = (short)0b11000000_11101010;
		short x = (short)0b01000000_11101010;
		Assert.assertThat(NumberProcessUtils.toUnsigned15bitsShort(s), is(x));
		
		s = (short)0b01000011_11101010;
		x = (short)0b01000011_11101010;
		Assert.assertThat(NumberProcessUtils.toUnsigned15bitsShort(s), is(x));
	}
	
	@Test
	public void testToUnsigned31bitsInteger() {
		int i = 0b11000000_11101010_11110000_10100011;
		int j = 0b01000000_11101010_11110000_10100011;
		Assert.assertThat(NumberProcessUtils.toUnsigned31bitsInteger(i), is(j));
	}
}
