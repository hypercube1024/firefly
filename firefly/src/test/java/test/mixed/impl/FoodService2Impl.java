package test.mixed.impl;

import test.mixed.Food;
import test.mixed.FoodService;
import test.mixed.FoodService2;

public class FoodService2Impl implements FoodService2 {

	private FoodService foodService;

	public void setFoodService(FoodService foodService) {
		this.foodService = foodService;
	}

	@Override
	public Food getFood(String name) {
		return foodService.getFood(name);
	}

}
