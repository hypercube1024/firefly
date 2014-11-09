package com.fireflysource.demo.service.impl;

import java.util.Map;

import com.fireflysource.demo.model.Fruit;
import com.fireflysource.demo.service.FruitService;

public class FruitServiceImpl implements FruitService {
	
	private Map<String, Fruit> map;
	
	public FruitServiceImpl(){}
	
	public FruitServiceImpl(Map<String, Fruit> map) {
		this.map = map;
	}

	@Override
	public Fruit getFruitByTitle(String title) {
		return map.get(title);
	}

}
