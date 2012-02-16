package com.firefly.utils;

abstract public class RandomUtils {
	public static final String ALL_CHAR = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	/**
	 * 生成min(包括)到max(包括)范围的随机数
	 * 
	 * @param min
	 *            随机数最小值
	 * @param max
	 *            随机数最大值
	 * @return min(包括)到max(包括)范围的随机数
	 */
	public static long random(long min, long max) {
		return Math.round(ThreadLocalRandom.current().nextDouble()
				* (max - min) + min);
	}

	/**
	 * 返回一个随机区段，例如：100:1:32:200:16:30，返回0的概率为100/(100+1+32+200+16+30)
	 * 
	 * @param conf
	 *            区段配置字符串
	 * @return 随机区段下标
	 */
	public static int randomSegment(String conf) {
		String[] tmp = StringUtils.split(conf, ":");
		int[] probability = new int[tmp.length];
		for (int i = 0; i < probability.length; i++)
			probability[i] = Integer.parseInt(tmp[i].trim());

		return randomSegment(probability);
	}

	/**
	 * 返回一个随机区段
	 * 
	 * @param probability
	 *            区段概率值
	 * @return 区段下标
	 */
	public static int randomSegment(int[] probability) {
		int total = 0;
		for (int i = 0; i < probability.length; i++) {
			total += probability[i];
			probability[i] = total;
		}
		int rand = (int) random(0, total - 1);
		for (int i = 0; i < probability.length; i++) {
			if (rand < probability[i]) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 生成随机字符串
	 * 
	 * @param length
	 *            生成字符串的长度
	 * @return 指定长度的随机字符串
	 */
	public static String randomString(int length) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			int index = (int) random(0, ALL_CHAR.length() - 1);
			sb.append(ALL_CHAR.charAt(index));
		}
		return sb.toString();
	}

	public static void main(String[] args) {
		String conf = "100:1:32:200:16:30";
		System.out.println(randomSegment(conf));

		System.out.println(random(0, 5));
		System.out.println(randomString(16));
	}
}
