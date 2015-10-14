package com.firefly.codec.http2.hpack;

import com.firefly.codec.http2.model.BadMessageException;
import com.firefly.codec.http2.model.HostPortHttpField;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpScheme;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.StaticTableHttpField;

public class MetaDataBuilder {
	private final int maxSize;
	private int size;
	private int status;
	private String method;
	private HttpScheme scheme;
	private HostPortHttpField authority;
	private String path;
	private long contentLength = Long.MIN_VALUE;

	private HttpFields fields = new HttpFields(10);

	/**
	 * @param maxHeadersSize
	 *            The maximum size of the headers, expressed as total name and
	 *            value characters.
	 */
	MetaDataBuilder(int maxHeadersSize) {
		maxSize = maxHeadersSize;
	}

	/**
	 * Get the maxSize.
	 * 
	 * @return the maxSize
	 */
	public int getMaxSize() {
		return maxSize;
	}

	/**
	 * Get the size.
	 * 
	 * @return the current size in bytes
	 */
	public int getSize() {
		return size;
	}

	public void emit(HttpField field) {
		int field_size = field.getName().length() + field.getValue().length();
		size += field_size;
		if (size > maxSize)
			throw new BadMessageException(HttpStatus.REQUEST_ENTITY_TOO_LARGE_413,
					"Header size " + size + ">" + maxSize);

		if (field instanceof StaticTableHttpField) {
			StaticTableHttpField value = (StaticTableHttpField) field;
			switch (field.getHeader()) {
			case C_STATUS:
				status = (Integer) value.getStaticValue();
				break;

			case C_METHOD:
				method = field.getValue();
				break;

			case C_SCHEME:
				scheme = (HttpScheme) value.getStaticValue();
				break;

			default:
				throw new IllegalArgumentException(field.getName());
			}
		} else if (field.getHeader() != null) {
			switch (field.getHeader()) {
			case C_STATUS:
				status = field.getIntValue();
				break;

			case C_METHOD:
				method = field.getValue();
				break;

			case C_SCHEME:
				scheme = HttpScheme.CACHE.get(field.getValue());
				break;

			case C_AUTHORITY:
				authority = (field instanceof HostPortHttpField) ? ((HostPortHttpField) field)
						: new AuthorityHttpField(field.getValue());
				break;

			case HOST:
				// :authority fields must come first. If we have one, ignore the
				// host header as far as authority goes.
				if (authority == null)
					authority = (field instanceof HostPortHttpField) ? ((HostPortHttpField) field)
							: new AuthorityHttpField(field.getValue());
				fields.add(field);
				break;

			case C_PATH:
				path = field.getValue();
				break;

			case CONTENT_LENGTH:
				contentLength = field.getLongValue();
				fields.add(field);
				break;

			default:
				if (field.getName().charAt(0) != ':')
					fields.add(field);
			}
		} else {
			if (field.getName().charAt(0) != ':')
				fields.add(field);
		}
	}

	public MetaData build() {
		try {
			HttpFields fields = this.fields;
			this.fields = new HttpFields(Math.max(10, fields.size() + 5));

			if (method != null)
				return new MetaData.Request(method, scheme, authority, path, HttpVersion.HTTP_2, fields, contentLength);
			if (status != 0)
				return new MetaData.Response(HttpVersion.HTTP_2, status, fields, contentLength);
			return new MetaData(HttpVersion.HTTP_2, fields, contentLength);
		} finally {
			status = 0;
			method = null;
			scheme = null;
			authority = null;
			path = null;
			size = 0;
			contentLength = Long.MIN_VALUE;
		}
	}

	/**
	 * Check that the max size will not be exceeded.
	 * 
	 * @param length
	 *            the length
	 * @param huffman
	 *            the huffman name
	 */
	public void checkSize(int length, boolean huffman) {
		// Apply a huffman fudge factor
		if (huffman)
			length = (length * 4) / 3;
		if ((size + length) > maxSize)
			throw new BadMessageException(HttpStatus.REQUEST_ENTITY_TOO_LARGE_413,
					"Header size " + (size + length) + ">" + maxSize);
	}
}
