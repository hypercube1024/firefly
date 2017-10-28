package com.firefly.example.ioc;

import java.util.List;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public interface BarService {

    List<String> getFoods();

    Map<String, Double> getFoodPrices();

    String getBarName();

    FooService getFooService();

}
