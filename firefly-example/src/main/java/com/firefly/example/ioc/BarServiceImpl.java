package com.firefly.example.ioc;

import java.util.List;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class BarServiceImpl implements BarService {

    private FooService fooService;
    private List<String> foods;
    private Map<String, Double> foodPrices;
    private String barName;

    @Override
    public List<String> getFoods() {
        return foods;
    }

    public void setFoods(List<String> foods) {
        this.foods = foods;
    }

    @Override
    public Map<String, Double> getFoodPrices() {
        return foodPrices;
    }

    public void setFoodPrices(Map<String, Double> foodPrices) {
        this.foodPrices = foodPrices;
    }

    public void setBarName(String barName) {
        this.barName = barName;
    }

    @Override
    public String getBarName() {
        return barName;
    }

    @Override
    public FooService getFooService() {
        return fooService;
    }

    public void setFooService(FooService fooService) {
        this.fooService = fooService;
    }

    public void init() {
        System.out.println("init BarService");
    }

    public void destroy() {
        System.out.println("destroy BarService");
    }

}
