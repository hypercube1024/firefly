package com.firefly.mvc.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.firefly.mvc.web.support.ControllerMetaInfo;
import com.firefly.mvc.web.support.URLParser;
import com.firefly.utils.pattern.Pattern;

public class Resource {
	public static final String WILDCARD = "?";
	private static final String[] EMPTY = new String[0];
	
	private String name;
	private Pattern pattern;
	private ControllerMetaInfo controller;
	private ResourceSet children = new ResourceSet();
	
	
	public ControllerMetaInfo getController() {
		return controller;
	}

	public void add(String uri, ControllerMetaInfo c) {
		Resource current = this;
		List<String> list = URLParser.parse(uri);
		int max = list.size() - 1;
		
		for (int i = 0; ;i++) {
			String name = list.get(i);
			if (i == max) {
				current = current.children.add(name, c);
				return;
			}
			
			current = current.children.add(name, null);;
		}
	}
	
	public Result match(String uri) {
		Result ret = null;
		Resource current = this;
		List<String> list = URLParser.parse(uri);
		List<String> params = new ArrayList<String>();
		
		for(String i : list) {
			ret = current.children.match(i);
			if(ret == null)
				return ret;
			
			if(ret.params != null) {
				for(String p : ret.params)
					params.add(p);
			}
				
			current = ret.resource;
		}
		
		ret.params = params.toArray(EMPTY);
		return ret;
	}
	
	public static class Result {
		public Resource resource;
		public String[] params;
		
		@Override
		public String toString() {
			return "Result [resource=" + resource + ", params="
					+ Arrays.toString(params) + "]";
		}

		public Result(Resource resource, String[] params) {
			this.resource = resource;
			this.params = params;
		}
	}
	
	private class ResourceSet implements Iterable<Resource>{
		private Map<String, Resource> map = new HashMap<String, Resource>();
		private List<Resource> list = new LinkedList<Resource>();
		
		private boolean isVariable() {
			return list.size() > 0;
		}
		
		private Result match(String str) {
			Resource ret = map.get(str);
			if(ret != null)
				return new Result(ret, null);
			
			for(Resource res : list) {
				String[] p = res.pattern.match(str);
				if(p != null)
					return new Result(res, p);
			}
			
			return null;
		}

		private Resource add(String name, ControllerMetaInfo c) {
			Resource resource = findByName(name);
			if(resource == null) {
				resource = new Resource();
				resource.name = name;
				
				if(name.contains(WILDCARD)) {
					resource.pattern = Pattern.compile(resource.name, WILDCARD);
					list.add(resource);
				} else {
					map.put(name, resource);
				}
			}
			if(c != null)
				resource.controller = c;
			return resource;
		}
		
		private Resource findByName(String name) {
			Resource r = map.get(name);
			if(r != null) {
				return r;
			} else {
				for(Resource res : list) {
					if(name.equals(res.name))
						return res;
				}
			}
			return null;
		}
		

		@Override
		public Iterator<Resource> iterator() {
			return new ResourceSetItr();
		}
		
		private class ResourceSetItr implements Iterator<Resource> {
			
			private Iterator<Resource> listItr = list.iterator();
			private Iterator<Entry<String, Resource>> mapItr = map.entrySet().iterator();

			@Override
			public boolean hasNext() {
				return mapItr.hasNext() || listItr.hasNext();
			}

			@Override
			public Resource next() {
				if(mapItr.hasNext())
					return mapItr.next().getValue();
				else
					return listItr.next();
			}

			@Override
			public void remove() {
				throw new RuntimeException("not implements this method!");
			}
			
		}
		
	}

	@Override
	public String toString() {
		return toString(" ", "");
	}
	
	private String toString(String l, String append) {
		StringBuilder s = new StringBuilder();
		s.append(append + name + "(" + children.isVariable() + ")" + "\r\n");
		for(Resource r : children) {
			s.append(l + r.toString(l + " ", "├"));
		}
		return s.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		Resource other = (Resource) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
