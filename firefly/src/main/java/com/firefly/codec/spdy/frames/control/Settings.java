package com.firefly.codec.spdy.frames.control;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Settings implements Iterable<Settings.Setting> {

	private final Map<ID, Settings.Setting> settings;

	public Settings() {
		settings = new HashMap<>();
	}

	public Settings(Settings original, boolean immutable) {
		Map<ID, Settings.Setting> copy = new HashMap<>(original.size());
		copy.putAll(original.settings);
		settings = immutable ? Collections.unmodifiableMap(copy) : copy;
	}

	public Setting get(ID id) {
		return settings.get(id);
	}

	public void put(Setting setting) {
		settings.put(setting.id(), setting);
	}

	public Setting remove(ID id) {
		return settings.remove(id);
	}

	public int size() {
		return settings.size();
	}

	public void clear() {
		settings.clear();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((settings == null) ? 0 : settings.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Settings other = (Settings) obj;
		if (settings == null) {
			if (other.settings != null)
				return false;
		} else if (!settings.equals(other.settings))
			return false;
		return true;
	}

	@Override
	public Iterator<Setting> iterator() {
		return settings.values().iterator();
	}

	@Override
	public String toString() {
		return settings.toString();
	}

	public static enum ID {

		UPLOAD_BANDWIDTH(1), 
		DOWNLOAD_BANDWIDTH(2), 
		ROUND_TRIP_TIME(3), 
		MAX_CONCURRENT_STREAMS(4), 
		CURRENT_CONGESTION_WINDOW(5), 
		DOWNLOAD_RETRANSMISSION_RATE(6), 
		INITIAL_WINDOW_SIZE(7), 
		CLIENT_CERTIFICATE_VECTOR_SIZE(8);

		public static ID from(int code) {
			return Codes.codes[code - 1];
		}

		private final int code;

		private ID(int code) {
			this.code = code;
			Codes.codes[code - 1] = this;
		}

		public int code() {
			return code;
		}

		@Override
		public String toString() {
			return String.valueOf(code);
		}

		private static class Codes {
			private static final ID[] codes = new ID[8];
		}
	}

	public static enum Flag {

		NONE((byte) 0), PERSIST((byte) 1), PERSISTED((byte) 2);

		public static Flag from(byte code) {
			return Codes.codes[code];
		}
		
		public static Flag from(int code) {
			return Codes.codes[code];
		}

		private final byte code;

		private Flag(byte code) {
			this.code = code;
			Codes.codes[code] = this;
		}

		public byte code() {
			return code;
		}

		private static class Codes {
			private static final Flag[] codes = new Flag[3];
		}
	}

	public static class Setting {
		private final ID id;
		private final Flag flag;
		private final int value;

		public Setting(ID id, int value) {
			this(id, Flag.NONE, value);
		}

		public Setting(ID id, Flag flag, int value) {
			this.id = id;
			this.flag = flag;
			this.value = value;
		}

		public ID id() {
			return id;
		}

		public Flag flag() {
			return flag;
		}

		public int value() {
			return value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((flag == null) ? 0 : flag.hashCode());
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + value;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Setting other = (Setting) obj;
			if (flag != other.flag)
				return false;
			if (id != other.id)
				return false;
			if (value != other.value)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Setting [id=" + id + ", flag=" + flag + ", value=" + value
					+ "]";
		}

	}
}
