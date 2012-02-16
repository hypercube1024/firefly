package test.utils;

import com.firefly.utils.StringUtils;

public class SplitPerformance {

	private static final int TIMES = 50000;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String str = "fdsf@dsfsdf";
		long start = System.currentTimeMillis();
		String [] strs = null;
		for (int i = 0; i < TIMES; i++) {
			strs = str.split("@");
			
		}
		long end = System.currentTimeMillis();
		System.out.println("String split [" + (end - start) + "ms] " + strs[0] + strs[1]);

		start = System.currentTimeMillis();
		for (int i = 0; i < TIMES; i++) {
			strs = StringUtils.split(str, "@");
		}
		end = System.currentTimeMillis();
		System.out.println("StringUtils split [" + (end - start) + "ms] " + strs[0] + strs[1]);
	}

}
