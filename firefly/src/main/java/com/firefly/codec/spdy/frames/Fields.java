package com.firefly.codec.spdy.frames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Fields {
	
	private final Map<String, Field> fields;
	
	public Fields(Map<String, Field> fields) {
		this.fields = fields;
	}
	
	public Field get(String name) {
		return fields.get(name.toLowerCase(Locale.ENGLISH));
	}
	
	public void put(String name, String value) {
		fields.put(name, new Field(name, value));
	}
	
	public void put(String name, Field field) {
		fields.put(name, field);
	}
	
	public void add(String name, String value) {
		String key = name.toLowerCase(Locale.ENGLISH);
		Field field = fields.get(key);
		if(field == null) {
			field = new Field(name, value);
			fields.put(name, field);
		} else {
			List<String> list = new ArrayList<>(field.getValues());
			list.add(value);
			fields.put(key, new Field(key, list));
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

		@Override
		public String toString() {
			return "Field [name=" + name + ", values=" + values + "]";
		}

	}
	
}
