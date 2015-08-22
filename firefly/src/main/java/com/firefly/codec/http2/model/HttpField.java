package com.firefly.codec.http2.model;

import java.util.ArrayList;
import java.util.Objects;

public class HttpField {
	private final static String __zeroquality = "q=0";
	private final HttpHeader header;
	private final String name;
	private final String value;
	// cached hashcode for case insensitive name
	private int hash = 0;

	public HttpField(HttpHeader header, String name, String value) {
		this.header = header;
		this.name = name;
		this.value = value;
	}

	public HttpField(HttpHeader header, String value) {
		this(header, header.asString(), value);
	}

	public HttpField(HttpHeader header, HttpHeaderValue value) {
		this(header, header.asString(), value.asString());
	}

	public HttpField(String name, String value) {
		this(HttpHeader.CACHE.get(name), name, value);
	}

	public HttpHeader getHeader() {
		return header;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public int getIntValue() {
		return Integer.valueOf(value);
	}

	public long getLongValue() {
		return Long.valueOf(value);
	}

	public String[] getValues() {
		ArrayList<String> list = new ArrayList<>();
		int state = 0;
		int start = 0;
		int end = 0;
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (state) {
			case 0: // initial white space
				switch (c) {
				case '"': // open quote
					state = 2;
					break;

				case ',': // ignore leading empty field
					break;

				case ' ': // more white space
				case '\t':
					break;

				default: // character
					start = i;
					end = i;
					state = 1;
				}
				break;

			case 1: // In token
				switch (c) {
				case ',': // next field
					list.add(value.substring(start, end + 1));
					state = 0;
					break;

				case ' ': // more white space
				case '\t':
					break;

				default:
					end = i;
				}
				break;

			case 2: // In Quoted
				switch (c) {
				case '\\': // next field
					state = 3;
					break;

				case '"': // end quote
					list.add(builder.toString());
					builder.setLength(0);
					state = 4;
					break;

				default:
					builder.append(c);
				}
				break;

			case 3: // In Quoted Quoted
				builder.append(c);
				state = 2;
				break;

			case 4: // WS after end quote
				switch (c) {
				case ' ': // white space
				case '\t': // white space
					break;

				case ',': // white space
					state = 0;
					break;

				default:
					throw new IllegalArgumentException("c=" + (int) c);

				}
				break;
			}
		}

		switch (state) {
		case 0:
			break;
		case 1:
			list.add(value.substring(start, end + 1));
			break;
		case 4:
			break;

		default:
			throw new IllegalArgumentException("state=" + state);
		}

		return list.toArray(new String[list.size()]);
	}

	/**
	 * Look for a value in a possible multi valued field
	 * 
	 * @param search
	 *            Values to search for
	 * @return True iff the value is contained in the field value entirely or as
	 *         an element of a quoted comma separated list. List element
	 *         parameters (eg qualities) are ignored, except if they are q=0, in
	 *         which case the item itself is ignored.
	 */
	public boolean contains(String search) {
		if (search == null)
			return value == null;
		if (search.length() == 0)
			return false;
		if (value == null)
			return false;

		int state = 0;
		int match = 0;
		int param = 0;

		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (state) {
			case 0: // initial white space
				switch (c) {
				case '"': // open quote
					match = 0;
					state = 2;
					break;

				case ',': // ignore leading empty field
					break;

				case ';': // ignore leading empty field parameter
					param = -1;
					match = -1;
					state = 5;
					break;

				case ' ': // more white space
				case '\t':
					break;

				default: // character
					match = c == search.charAt(0) ? 1 : -1;
					state = 1;
					break;
				}
				break;

			case 1: // In token
				switch (c) {
				case ',': // next field
					// Have we matched the token?
					if (match == search.length())
						return true;
					state = 0;
					break;

				case ';':
					param = match >= 0 ? 0 : -1;
					state = 5; // parameter
					break;

				default:
					if (match > 0) {
						if (match < search.length())
							match = c == search.charAt(match) ? (match + 1)
									: -1;
						else if (c != ' ' && c != '\t')
							match = -1;
					}
					break;

				}
				break;

			case 2: // In Quoted token
				switch (c) {
				case '\\': // quoted character
					state = 3;
					break;

				case '"': // end quote
					state = 4;
					break;

				default:
					if (match >= 0) {
						if (match < search.length())
							match = c == search.charAt(match) ? (match + 1)
									: -1;
						else
							match = -1;
					}
				}
				break;

			case 3: // In Quoted character in quoted token
				if (match >= 0) {
					if (match < search.length())
						match = c == search.charAt(match) ? (match + 1) : -1;
					else
						match = -1;
				}
				state = 2;
				break;

			case 4: // WS after end quote
				switch (c) {
				case ' ': // white space
				case '\t': // white space
					break;

				case ';':
					state = 5; // parameter
					break;

				case ',': // end token
					// Have we matched the token?
					if (match == search.length())
						return true;
					state = 0;
					break;

				default:
					// This is an illegal token, just ignore
					match = -1;
				}
				break;

			case 5: // parameter
				switch (c) {
				case ',': // end token
					// Have we matched the token and not q=0?
					if (param != __zeroquality.length()
							&& match == search.length())
						return true;
					param = 0;
					state = 0;
					break;

				case ' ': // white space
				case '\t': // white space
					break;

				default:
					if (param >= 0) {
						if (param < __zeroquality.length())
							param = c == __zeroquality.charAt(param) ? (param + 1)
									: -1;
						else if (c != '0' && c != '.')
							param = -1;
					}

				}
				break;

			default:
				throw new IllegalStateException();
			}
		}

		return param != __zeroquality.length() && match == search.length();
	}

	@Override
	public String toString() {
		String v = getValue();
		return getName() + ": " + (v == null ? "" : v);
	}

	public boolean isSameName(HttpField field) {
		if (field == null)
			return false;
		if (field == this)
			return true;
		if (header != null && header == field.getHeader())
			return true;
		if (name.equalsIgnoreCase(field.getName()))
			return true;
		return false;
	}

	private int nameHashCode() {
		int h = this.hash;
		int len = name.length();
		if (h == 0 && len > 0) {
			for (int i = 0; i < len; i++) {
				// simple case insensitive hash
				char c = name.charAt(i);
				// assuming us-ascii (per last paragraph on
				// http://tools.ietf.org/html/rfc7230#section-3.2.4)
				if ((c >= 'a' && c <= 'z'))
					c -= 0x20;
				h = 31 * h + c;
			}
			this.hash = h;
		}
		return h;
	}

	@Override
	public int hashCode() {
		if (header == null)
			return value.hashCode() ^ nameHashCode();
		return value.hashCode() ^ header.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof HttpField))
			return false;
		HttpField field = (HttpField) o;
		if (header != field.getHeader())
			return false;
		if (!name.equalsIgnoreCase(field.getName()))
			return false;
		if (value == null && field.getValue() != null)
			return false;
		return Objects.equals(value, field.getValue());
	}

	public static class IntValueHttpField extends HttpField {
		private final int _int;

		public IntValueHttpField(HttpHeader header, String name, String value,
				int intValue) {
			super(header, name, value);
			_int = intValue;
		}

		public IntValueHttpField(HttpHeader header, String name, String value) {
			this(header, name, value, Integer.valueOf(value));
		}

		public IntValueHttpField(HttpHeader header, String name, int intValue) {
			this(header, name, Integer.toString(intValue), intValue);
		}

		public IntValueHttpField(HttpHeader header, int value) {
			this(header, header.asString(), value);
		}

		@Override
		public int getIntValue() {
			return _int;
		}

		@Override
		public long getLongValue() {
			return _int;
		}
	}

	public static class LongValueHttpField extends HttpField {
		private final long _long;

		public LongValueHttpField(HttpHeader header, String name, String value,
				long longValue) {
			super(header, name, value);
			_long = longValue;
		}

		public LongValueHttpField(HttpHeader header, String name, String value) {
			this(header, name, value, Long.valueOf(value));
		}

		public LongValueHttpField(HttpHeader header, String name, long value) {
			this(header, name, Long.toString(value), value);
		}

		public LongValueHttpField(HttpHeader header, long value) {
			this(header, header.asString(), value);
		}

		@Override
		public int getIntValue() {
			return (int) _long;
		}

		@Override
		public long getLongValue() {
			return _long;
		}
	}
}
