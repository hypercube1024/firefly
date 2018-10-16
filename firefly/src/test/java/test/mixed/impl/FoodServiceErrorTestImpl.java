package test.mixed.impl;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import test.mixed.Food;
import test.mixed.FoodRepository;
import test.mixed.FoodService;

@Component("foodServiceErrorTest")
public class FoodServiceErrorTestImpl implements FoodService {

    @Inject
    private FoodRepository foodRepository;

    @Override
    public Food getFood(String name) {
        return foodRepository.getFood().get(0);
    }

}
