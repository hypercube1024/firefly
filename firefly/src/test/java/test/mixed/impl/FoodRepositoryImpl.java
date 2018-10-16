package test.mixed.impl;

import test.mixed.Food;
import test.mixed.FoodRepository;

import java.util.List;

public class FoodRepositoryImpl implements FoodRepository {

    private List<Food> food;

    public FoodRepositoryImpl() {
    }

    public FoodRepositoryImpl(List<Food> food) {
        this.food = food;
    }

    @Override
    public List<Food> getFood() {
        return food;
    }

    public void setFood(List<Food> food) {
        this.food = food;
    }

}
