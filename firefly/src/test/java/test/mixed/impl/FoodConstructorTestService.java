package test.mixed.impl;

import test.ioc.TestConstructorsIoc.BeanTest;
import test.mixed.FoodRepository;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;

@Component
public class FoodConstructorTestService {

	private BeanTest beanTest;
	private FoodRepository foodRepository;

	public FoodConstructorTestService() {

	}

	@Inject
	public FoodConstructorTestService(BeanTest beanTest,
			FoodRepository foodRepository) {
		this.beanTest = beanTest;
		this.foodRepository = foodRepository;
	}

	public BeanTest getBeanTest() {
		return beanTest;
	}

	public void setBeanTest(BeanTest beanTest) {
		this.beanTest = beanTest;
	}

	public FoodRepository getFoodRepository() {
		return foodRepository;
	}

	public void setFoodRepository(FoodRepository foodRepository) {
		this.foodRepository = foodRepository;
	}

}
