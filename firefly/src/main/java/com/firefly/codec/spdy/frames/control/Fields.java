package com.firefly.codec.spdy.frames.control;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.firefly.codec.spdy.frames.Serialization;

public class Fields implements Serialization, Iterable<Fields.Field> {
	
	private final HeadersBlockGenerator headersBlockGenerator;
	private final Map<String, Field> fields;
	
	public Fields(Map<String, Field> fields, HeadersBlockGenerator headersBlockGenerator) {
		this.fields = fields;
		this.headersBlockGenerator = headersBlockGenerator;
	}
	
	public Field get(String name) {
		return fields.get(name.toLowerCase(Locale.ENGLISH));
	}
	
	public void put(String name, String value) {
		put(name, new Field(name, value));
	}
	
	public void put(String name, Field field) {
		fields.put(name.toLowerCase(Locale.ENGLISH), field);
	}
	
	public void add(String name, String value) {
		Field field = get(name);
		if(field == null) {
			put(name, value);
		} else {
			List<String> list = new ArrayList<>(field.getValues());
			list.add(value);
			put(name, new Field(name, list));
		}
	}
	
	public Field remove(String name) {
        return fields.remove(name.toLowerCase(Locale.ENGLISH));
    }
	
	public void clear() {
        fields.clear();
    }
	
	public boolean isEmpty() {
        return fields.isEmpty();
    }
	
	public int getSize() {
        return fields.size();
    }

	@Override
	public String toString() {
		return "Fields [fields=" + fields + "]";
	}

	public static class Field {
		private final String name;
		private final List<String> values;

		public Field(String name, String value) {
			this(name, Collections.singletonList(value));
		}

		public Field(String name, List<String> values) {
			this.name = name.toLowerCase(Locale.ENGLISH);
			this.values = values;
		}

		public String getName() {
			return name;
		}

		public List<String> getValues() {
			return values;
		}

		public String getValue() {
			return values.get(0);
		}
		
		public boolean hasMultipleValues(){
            return values.size() > 1;
        }
		
		@Override
		public String toString() {
			return "Field [name=" + name + ", values=" + values + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result
					+ ((values == null) ? 0 : values.hashCode());
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
			Field other = (Field) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (values == null) {
				if (other.values != null)
					return false;
			} else if (!values.equals(other.values))
				return false;
			return true;
		}

	}

	@Override
	public Iterator<Field> iterator() {
		return fields.values().iterator();
	}
	
	@Override
	public ByteBuffer toByteBuffer() {
		return headersBlockGenerator.generate(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
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
		Fields other = (Fields) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		return true;
	}
	
}
