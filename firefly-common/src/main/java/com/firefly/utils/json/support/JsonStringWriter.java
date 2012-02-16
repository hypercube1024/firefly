package com.firefly.utils.json.support;

import java.util.Deque;
import java.util.LinkedList;

import com.firefly.utils.io.IOUtils;
import com.firefly.utils.io.StringWriter;
import static com.firefly.utils.json.JsonStringSymbol.QUOTE;
import static com.firefly.utils.json.JsonStringSymbol.ARRAY_PRE;
import static com.firefly.utils.json.JsonStringSymbol.ARRAY_SUF;
import static com.firefly.utils.json.JsonStringSymbol.SEPARATOR;

public class JsonStringWriter extends StringWriter {

	private Deque<Object> deque = new LinkedList<Object>();

	public void pushRef(Object obj) {
		deque.addFirst(obj);
	}

	public boolean existRef(Object obj) {
		return deque.contains(obj);
	}

	public void popRef() {
		deque.removeFirst();
	}

	private void writeJsonString0(String value) {
		buf[count++] = QUOTE;
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			switch (ch) {
			case '\b':
				buf[count++] = '\\';
				buf[count++] = 'b';
				break;
			case '\n':
				buf[count++] = '\\';
				buf[count++] = 'n';
				break;
			case '\r':
				buf[count++] = '\\';
				buf[count++] = 'r';
				break;
			case '\f':
				buf[count++] = '\\';
				buf[count++] = 'f';
				break;
			case '\\':
				buf[count++] = '\\';
				buf[count++] = '\\';
				break;
			case '"':
				buf[count++] = '\\';
				buf[count++] = '"';
				break;
			case '\t':
				buf[count++] = '\\';
				buf[count++] = 't';
				break;

			default:
				buf[count++] = ch;
				break;
			}
		}
		buf[count++] = QUOTE;
	}
	
	private void writeJsonString0NoFilter(String value) {
		int len = value.length();
		buf[count++] = QUOTE;
		value.getChars(0, len, buf, count);
		count += len;
		buf[count++] = QUOTE;
	}

	public void writeStringWithQuoteNoFilter(String value) {
		int newcount = count + value.length() + 2;
		if (newcount > buf.length) {
			expandCapacity(newcount);
		}
		writeJsonString0NoFilter(value);
	}
	
	public void writeStringArrayNoFilter(String[] array) {
		int arrayLen = array.length;
		if (arrayLen == 0) {
			buf[count++] = ARRAY_PRE;
			buf[count++] = ARRAY_SUF;
			return;
		}
		
		int iMax = arrayLen - 1;
		int totalSize = 2;
		for (int i = 0; i < arrayLen; i++) {
			totalSize += array[i].length() + 2 + 1;
		}

		int newcount = count + totalSize;
		if (newcount > buf.length) {
			expandCapacity(newcount);
		}

		buf[count++] = ARRAY_PRE;
		for (int i = 0; ; ++i) {
			writeJsonString0NoFilter(array[i]);
			if (i == iMax) {
				buf[count++] = ARRAY_SUF;
				return;
			}
			buf[count++] = SEPARATOR;
		}
	}
	
	public void writeStringWithQuote(String value) {
		int newcount = count + value.length() * 2 + 2;
		if (newcount > buf.length) {
			expandCapacity(newcount);
		}
		writeJsonString0(value);
	}

	public void writeStringArray(String[] array) {
		int arrayLen = array.length;
		if (arrayLen == 0) {
			buf[count++] = ARRAY_PRE;
			buf[count++] = ARRAY_SUF;
			return;
		}
		
		int iMax = arrayLen - 1;
		int totalSize = 2;
		for (int i = 0; i < arrayLen; i++) {
			totalSize += array[i].length() * 2 + 2 + 1;
		}

		int newcount = count + totalSize;
		if (newcount > buf.length) {
			expandCapacity(newcount);
		}

		buf[count++] = ARRAY_PRE;
		for (int i = 0; ; ++i) {
			writeJsonString0(array[i]);
			if (i == iMax) {
				buf[count++] = ARRAY_SUF;
				return;
			}
			buf[count++] = SEPARATOR;
		}
	}

	public void writeIntArray(int[] array) {
		int arrayLen = array.length;
		if (arrayLen == 0) {
			buf[count++] = ARRAY_PRE;
			buf[count++] = ARRAY_SUF;
			return;
		}
		int iMax = arrayLen - 1;
		// System.out.println("current count: " + count);
		int elementMaxLen = MIN_INT_VALUE.length;
		int newcount = count + (elementMaxLen + 1) * arrayLen + 2 - 1;
		if (newcount > buf.length) {
			expandCapacity(newcount);
		}

		buf[count++] = ARRAY_PRE;
		for (int i = 0;; i++) {
			int val = array[i];
			if (val == Integer.MIN_VALUE) {
				System.arraycopy(MIN_INT_VALUE, 0, buf, count, elementMaxLen);
				count += elementMaxLen;
			} else {
				count += (val < 0) ? IOUtils.stringSize(-val) + 1 : IOUtils
						.stringSize(val);
				IOUtils.getChars(val, count, buf);
			}

			if (i == iMax) {
				buf[count++] = ARRAY_SUF;
				// System.out.println("current count: " + count);
				return;
			}
			buf[count++] = SEPARATOR;
		}
	}

	public void writeIntArray(Integer[] array) {
		int arrayLen = array.length;
		if (arrayLen == 0) {
			buf[count++] = ARRAY_PRE;
			buf[count++] = ARRAY_SUF;
			return;
		}
		int iMax = arrayLen - 1;
		// System.out.println("current count: " + count);
		int elementMaxLen = MIN_INT_VALUE.length;
		int newcount = count + (elementMaxLen + 1) * arrayLen + 2 - 1;
		if (newcount > buf.length) {
			expandCapacity(newcount);
		}

		buf[count++] = ARRAY_PRE;
		for (int i = 0;; i++) {
			int val = array[i];
			if (val == Integer.MIN_VALUE) {
				System.arraycopy(MIN_INT_VALUE, 0, buf, count, elementMaxLen);
				count += elementMaxLen;
			} else {
				count += (val < 0) ? IOUtils.stringSize(-val) + 1 : IOUtils
						.stringSize(val);
				IOUtils.getChars(val, count, buf);
			}

			if (i == iMax) {
				buf[count++] = ARRAY_SUF;
				// System.out.println("current count: " + count);
				return;
			}
			buf[count++] = SEPARATOR;
		}
	}

	public void writeShortArray(short[] array) {
		int arrayLen = array.length;
		if (arrayLen == 0) {
			buf[count++] = ARRAY_PRE;
			buf[count++] = ARRAY_SUF;
			return;
		}
		int iMax = arrayLen - 1;
		// System.out.println("current count: " + count);
		int elementMaxLen = MIN_INT_VALUE.length;
		int newcount = count + (elementMaxLen + 1) * arrayLen + 2 - 1;
		if (newcount > buf.length) {
			expandCapacity(newcount);
		}

		buf[count++] = ARRAY_PRE;
		for (int i = 0;; i++) {
			int val = array[i];
			if (val == Integer.MIN_VALUE) {
				System.arraycopy(MIN_INT_VALUE, 0, buf, count, elementMaxLen);
				count += elementMaxLen;
			} else {
				count += (val < 0) ? IOUtils.stringSize(-val) + 1 : IOUtils
						.stringSize(val);
				IOUtils.getChars(val, count, buf);
			}

			if (i == iMax) {
				buf[count++] = ARRAY_SUF;
				// System.out.println("current count: " + count);
				return;
			}
			buf[count++] = SEPARATOR;
		}
	}

	public void writeShortArray(Short[] array) {
		int arrayLen = array.length;
		if (arrayLen == 0) {
			buf[count++] = ARRAY_PRE;
			buf[count++] = ARRAY_SUF;
			return;
		}
		int iMax = arrayLen - 1;
		// System.out.println("current count: " + count);
		int elementMaxLen = MIN_INT_VALUE.length;
		int newcount = count + (elementMaxLen + 1) * arrayLen + 2 - 1;
		if (newcount > buf.length) {
			expandCapacity(newcount);
		}

		buf[count++] = ARRAY_PRE;
		for (int i = 0;; i++) {
			int val = array[i];
			if (val == Integer.MIN_VALUE) {
				System.arraycopy(MIN_INT_VALUE, 0, buf, count, elementMaxLen);
				count += elementMaxLen;
			} else {
				count += (val < 0) ? IOUtils.stringSize(-val) + 1 : IOUtils
						.stringSize(val);
				IOUtils.getChars(val, count, buf);
			}

			if (i == iMax) {
				buf[count++] = ARRAY_SUF;
				// System.out.println("current count: " + count);
				return;
			}
			buf[count++] = SEPARATOR;
		}
	}

	public void writeLongArray(long[] array) {
		int arrayLen = array.length;
		if (arrayLen == 0) {
			buf[count++] = ARRAY_PRE;
			buf[count++] = ARRAY_SUF;
			return;
		}
		int iMax = arrayLen - 1;
		// System.out.println("current count: " + count);
		int elementMaxLen = MIN_LONG_VALUE.length;
		int newcount = count + (elementMaxLen + 1) * arrayLen + 2 - 1;
		if (newcount > buf.length) {
			expandCapacity(newcount);
		}

		buf[count++] = ARRAY_PRE;
		for (int i = 0;; i++) {
			long val = array[i];
			if (val == Long.MIN_VALUE) {
				System.arraycopy(MIN_LONG_VALUE, 0, buf, count, elementMaxLen);
				count += elementMaxLen;
			} else {
				count += (val < 0) ? IOUtils.stringSize(-val) + 1 : IOUtils
						.stringSize(val);
				IOUtils.getChars(val, count, buf);
			}

			if (i == iMax) {
				buf[count++] = ARRAY_SUF;
				// System.out.println("current count: " + count);
				return;
			}
			buf[count++] = SEPARATOR;
		}
	}

	public void writeLongArray(Long[] array) {
		int arrayLen = array.length;
		if (arrayLen == 0) {
			buf[count++] = ARRAY_PRE;
			buf[count++] = ARRAY_SUF;
			return;
		}
		int iMax = arrayLen - 1;
		// System.out.println("current count: " + count);
		int elementMaxLen = MIN_LONG_VALUE.length;
		int newcount = count + (elementMaxLen + 1) * arrayLen + 2 - 1;
		if (newcount > buf.length) {
			expandCapacity(newcount);
		}

		buf[count++] = ARRAY_PRE;
		for (int i = 0;; i++) {
			long val = array[i];
			if (val == Long.MIN_VALUE) {
				System.arraycopy(MIN_LONG_VALUE, 0, buf, count, elementMaxLen);
				count += elementMaxLen;
			} else {
				count += (val < 0) ? IOUtils.stringSize(-val) + 1 : IOUtils
						.stringSize(val);
				IOUtils.getChars(val, count, buf);
			}

			if (i == iMax) {
				buf[count++] = ARRAY_SUF;
				// System.out.println("current count: " + count);
				return;
			}
			buf[count++] = SEPARATOR;
		}
	}

	public void writeBooleanArray(boolean[] array) {
		int arrayLen = array.length;
		if (arrayLen == 0) {
			buf[count++] = ARRAY_PRE;
			buf[count++] = ARRAY_SUF;
			return;
		}
		int iMax = arrayLen - 1;
		int newcount = count + (5 + 1) * arrayLen + 2 - 1;
		if (newcount > buf.length) {
			expandCapacity(newcount);
		}

		// System.out.println("current count: " + count);
		buf[count++] = ARRAY_PRE;
		for (int i = 0;; i++) {
			if (array[i]) {
				buf[count++] = 't';
				buf[count++] = 'r';
				buf[count++] = 'u';
				buf[count++] = 'e';
			} else {
				buf[count++] = 'f';
				buf[count++] = 'a';
				buf[count++] = 'l';
				buf[count++] = 's';
				buf[count++] = 'e';
			}
			if (i == iMax) {
				buf[count++] = ARRAY_SUF;
				// System.out.println("current count: " + count);
				return;
			}
			buf[count++] = SEPARATOR;
		}
	}

	public void writeBooleanArray(Boolean[] array) {
		int arrayLen = array.length;
		if (arrayLen == 0) {
			buf[count++] = ARRAY_PRE;
			buf[count++] = ARRAY_SUF;
			return;
		}
		int iMax = arrayLen - 1;
		int newcount = count + (5 + 1) * arrayLen + 2 - 1;
		if (newcount > buf.length) {
			expandCapacity(newcount);
		}

		// System.out.println("current count: " + count);
		buf[count++] = ARRAY_PRE;
		for (int i = 0;; i++) {
			if (array[i]) {
				buf[count++] = 't';
				buf[count++] = 'r';
				buf[count++] = 'u';
				buf[count++] = 'e';
			} else {
				buf[count++] = 'f';
				buf[count++] = 'a';
				buf[count++] = 'l';
				buf[count++] = 's';
				buf[count++] = 'e';
			}
			if (i == iMax) {
				buf[count++] = ARRAY_SUF;
				// System.out.println("current count: " + count);
				return;
			}
			buf[count++] = SEPARATOR;
		}
	}
}
