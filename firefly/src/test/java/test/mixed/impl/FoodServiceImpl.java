package test.mixed.impl;

import test.mixed.Food;
import test.mixed.FoodRepository;
import test.mixed.FoodService;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;

@Component("foodService")
public class FoodServiceImpl implements FoodService {

    @Inject("foodRepository")
    private FoodRepository foodRepository;

    @Override
    public Food getFood(String name) {
        for (Food f : foodRepository.getFood()) {
            if (f.getName().equals(name))
                return f;
        }
        return null;
    }

}
