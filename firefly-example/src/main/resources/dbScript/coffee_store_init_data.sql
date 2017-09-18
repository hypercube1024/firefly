INSERT INTO `coffee_store`.`product` (`type`, `name`, `price`, `status`, `description`, `create_time`, `update_time`)
VALUES
  (1, 'Cappuccino', 22.99, 1, 'cappuccino 01', now(), now()),
  (1, 'Coffee latte', 23.99, 1,'latte 01', now(), now()),
  (1, 'American-style coffee', 18.5, 1, 'American-style coffee 01', now(), now()),
  (1, 'Vanilla Latte', 23.99, 1, 'Vanilla Latte 01', now(), now()),
  (1, 'Mocha', 21, 1, 'Mocha 01', now(), now()),
  (1, 'Coconut Mocha', 21.99, 1, 'Coconut Mocha 01', now(), now()),
  (2, 'Tiramisu', 25.8, 1, 'Tiramisu 01', now(), now())
;

INSERT INTO `coffee_store`.`inventory` (`amount`, `product_id`, `create_time`, `update_time`)
VALUES
  (100, 1, now(), now()),
  (100, 2, now(), now()),
  (100, 3, now(), now()),
  (77, 4, now(), now()),
  (100, 5, now(), now()),
  (100, 6, now(), now()),
  (80, 7, now(), now())
;