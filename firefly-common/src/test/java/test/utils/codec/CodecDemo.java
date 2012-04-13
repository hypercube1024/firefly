package test.utils.codec;

import com.firefly.utils.codec.Base64;

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
	}

}
