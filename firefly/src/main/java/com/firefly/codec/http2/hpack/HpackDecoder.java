package com.firefly.codec.http2.hpack;

import java.nio.ByteBuffer;

import com.firefly.codec.http2.hpack.HpackContext.Entry;
import com.firefly.codec.http2.model.BadMessageException;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.lang.TypeUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

/**
 * Hpack Decoder
 * <p>
 * This is not thread safe and may only be called by 1 thread at a time
 */
public class HpackDecoder {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	public final static HttpField.LongValueHttpField CONTENT_LENGTH_0 = new HttpField.LongValueHttpField(
			HttpHeader.CONTENT_LENGTH, 0L);

	private final HpackContext context;
	private final MetaDataBuilder builder;
	private int localMaxDynamicTableSize;

	/**
	 * @param localMaxDynamicTableSize
	 *            The maximum allowed size of the local dynamic header field
	 *            table.
	 * @param maxHeaderSize
	 *            The maximum allowed size of a headers block, expressed as
	 *            total of all name and value characters.
	 */
	public HpackDecoder(int localMaxDynamicTableSize, int maxHeaderSize) {
		context = new HpackContext(localMaxDynamicTableSize);
		this.localMaxDynamicTableSize = localMaxDynamicTableSize;
		builder = new MetaDataBuilder(maxHeaderSize);
	}

	public HpackContext getHpackContext() {
		return context;
	}

	public void setLocalMaxDynamicTableSize(int localMaxdynamciTableSize) {
		this.localMaxDynamicTableSize = localMaxdynamciTableSize;
	}

	public MetaData decode(ByteBuffer buffer) {
		if (log.isDebugEnable())
			log.debug(String.format("CtxTbl[%x] decoding %d octets", context.hashCode(), buffer.remaining()));

		// If the buffer is big, don't even think about decoding it
		if (buffer.remaining() > builder.getMaxSize())
			throw new BadMessageException(HttpStatus.REQUEST_ENTITY_TOO_LARGE_413,
					"Header frame size " + buffer.remaining() + ">" + builder.getMaxSize());

		while (buffer.hasRemaining()) {
			if (log.isDebugEnable()) {
				int l = Math.min(buffer.remaining(), 16);
				// TODO: not guaranteed the buffer has a backing array !
				log.debug(
						"decode  " + TypeUtils.toHexString(buffer.array(), buffer.arrayOffset() + buffer.position(), l)
								+ (l < buffer.remaining() ? "..." : ""));
			}

			byte b = buffer.get();
			if (b < 0) {
				// 7.1 indexed if the high bit is set
				int index = NBitInteger.decode(buffer, 7);
				Entry entry = context.get(index);
				if (entry == null) {
					throw new BadMessageException("Unknown index " + index);
				} else if (entry.isStatic()) {
					if (log.isDebugEnable())
						log.debug("decode IdxStatic {}", entry);
					// emit field
					builder.emit(entry.getHttpField());

					// TODO copy and add to reference set if there is room
					// _context.add(entry.getHttpField());
				} else {
					if (log.isDebugEnable())
						log.debug("decode Idx {}", entry);
					// emit
					builder.emit(entry.getHttpField());
				}
			} else {
				// look at the first nibble in detail
				byte f = (byte) ((b & 0xF0) >> 4);
				String name;
				HttpHeader header;
				String value;

				boolean indexed;
				int name_index;

				switch (f) {
				case 2: // 7.3
				case 3: // 7.3
					// change table size
					int size = NBitInteger.decode(buffer, 5);
					if (log.isDebugEnable())
						log.debug("decode resize=" + size);
					if (size > localMaxDynamicTableSize)
						throw new IllegalArgumentException();
					context.resize(size);
					continue;

				case 0: // 7.2.2
				case 1: // 7.2.3
					indexed = false;
					name_index = NBitInteger.decode(buffer, 4);
					break;

				case 4: // 7.2.1
				case 5: // 7.2.1
				case 6: // 7.2.1
				case 7: // 7.2.1
					indexed = true;
					name_index = NBitInteger.decode(buffer, 6);
					break;

				default:
					throw new IllegalStateException();
				}

				boolean huffmanName = false;

				// decode the name
				if (name_index > 0) {
					Entry name_entry = context.get(name_index);
					name = name_entry.getHttpField().getName();
					header = name_entry.getHttpField().getHeader();
				} else {
					huffmanName = (buffer.get() & 0x80) == 0x80;
					int length = NBitInteger.decode(buffer, 7);
					builder.checkSize(length, huffmanName);
					if (huffmanName)
						name = Huffman.decode(buffer, length);
					else
						name = toASCIIString(buffer, length);
					for (int i = 0; i < name.length(); i++) {
						char c = name.charAt(i);
						if (c >= 'A' && c <= 'Z') {
							throw new BadMessageException(400, "Uppercase header name");
						}
					}
					header = HttpHeader.CACHE.get(name);
				}

				// decode the value
				boolean huffmanValue = (buffer.get() & 0x80) == 0x80;
				int length = NBitInteger.decode(buffer, 7);
				builder.checkSize(length, huffmanValue);
				if (huffmanValue)
					value = Huffman.decode(buffer, length);
				else
					value = toASCIIString(buffer, length);

				// Make the new field
				HttpField field;
				if (header == null) {
					// just make a normal field and bypass header name lookup
					field = new HttpField(null, name, value);
				} else {
					// might be worthwhile to create a value HttpField if it is
					// indexed
					// and/or of a type that may be looked up multiple times.
					switch (header) {
					case C_STATUS:
						if (indexed)
							field = new HttpField.IntValueHttpField(header, name, value);
						else
							field = new HttpField(header, name, value);
						break;

					case C_AUTHORITY:
						field = new AuthorityHttpField(value);
						break;

					case CONTENT_LENGTH:
						if ("0".equals(value))
							field = CONTENT_LENGTH_0;
						else
							field = new HttpField.LongValueHttpField(header, name, value);
						break;

					default:
						field = new HttpField(header, name, value);
						break;
					}
				}

				if (log.isDebugEnable())
					log.debug("decoded '" + field + "' by Lit"
							+ (name_index > 0 ? "IdxName" : (huffmanName ? "HuffName" : "LitName"))
							+ (huffmanValue ? "HuffVal" : "LitVal") + (indexed ? "Idx" : ""));

				// emit the field
				builder.emit(field);

				// if indexed
				if (indexed) {
					// add to dynamic table
					context.add(field);
				}

			}
		}

		return builder.build();
	}

	public static String toASCIIString(ByteBuffer buffer, int length) {
		StringBuilder builder = new StringBuilder(length);
		int start = buffer.arrayOffset() + buffer.position();
		int end = start + length;
		buffer.position(end);
		byte[] array = buffer.array();
		for (int i = start; i < end; i++)
			builder.append((char) (0x7f & array[i]));
		return builder.toString();
	}

	@Override
	public String toString() {
		return String.format("HpackDecoder@%x{%s}", hashCode(), context);
	}
}
