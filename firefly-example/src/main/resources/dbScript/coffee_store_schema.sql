DROP SCHEMA IF EXISTS coffee_store;
CREATE SCHEMA coffee_store;
SET MODE MYSQL ;

CREATE TABLE `coffee_store`.`user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY ,
  `name` VARCHAR(64) NOT NULL UNIQUE ,
  `password` VARCHAR(64) NOT NULL ,
  `create_time` DATETIME NOT NULL,
  `update_time` DATETIME NOT NULL
);
CREATE INDEX `idx_user_name` ON `coffee_store`.`user` (`name`);

CREATE TABLE `coffee_store`.`product` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY ,
  `type` INT NOT NULL ,
  `name` VARCHAR(64) NOT NULL UNIQUE ,
  `status` INT NOT NULL DEFAULT 0 ,
  `price` DECIMAL NOT NULL ,
  `description` VARCHAR(200) NOT NULL ,
  `create_time` DATETIME NOT NULL ,
  `update_time` DATETIME NOT NULL
);
CREATE INDEX `idx_product_name` ON `coffee_store`.`product` (`name`);

CREATE TABLE `coffee_store`.`order` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY ,
  `status` INT NOT NULL DEFAULT 0,
  `number` INT NOT NULL DEFAULT 0,
  `price` DECIMAL NOT NULL DEFAULT 0.0,
  `total_price` DECIMAL NOT NULL DEFAULT 0.0,
  `product_id` BIGINT NOT NULL DEFAULT 0,
  `user_id` BIGINT NOT NULL DEFAULT 0,
  `description` VARCHAR(200) NOT NULL ,
  `create_time` DATETIME NOT NULL ,
  `update_time` DATETIME NOT NULL
);
CREATE INDEX `idx_order_user_id` ON `coffee_store`.`order` (`user_id`);
CREATE INDEX `idx_order_product_id` ON `coffee_store`.`order` (`product_id`);

CREATE TABLE `coffee_store`.`inventory` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY ,
  `amount` BIGINT NOT NULL DEFAULT 0,
  `product_id` BIGINT NOT NULL DEFAULT 0 UNIQUE ,
  `create_time` DATETIME NOT NULL ,
  `update_time` DATETIME NOT NULL
);
CREATE INDEX `idx_inventory_product_id` ON `coffee_store`.`inventory` (`product_id`);