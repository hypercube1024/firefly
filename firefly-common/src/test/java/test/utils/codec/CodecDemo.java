package test.utils.codec;

import java.util.Arrays;

import com.firefly.utils.codec.Base64;
import com.firefly.utils.codec.HexUtils;

public class CodecDemo {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		StringBuilder strBuilder = new StringBuilder(100);
		for (int i = 0; i < 100; i++) {
			strBuilder.append(i).append("+");
		}

		String ret = Base64.encodeToString(strBuilder.toString().getBytes(), false);
		System.out.println(ret);
		
		byte[] b = Base64.decode(ret);
		System.out.println(new String(b));
		
		byte[] b1 = {-128, 3};
		String s = HexUtils.bytesToHex(b1);
		System.out.println(s);
		System.out.println(Arrays.toString(HexUtils.hexToBytes(s)));
	}

}
