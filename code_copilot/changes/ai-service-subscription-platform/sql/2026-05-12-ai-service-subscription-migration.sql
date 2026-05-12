-- AI 服务订阅与营销平台增量迁移脚本。
-- 用于已有本地库验证；只新增字段/表和修正示例套餐数据，不执行 DROP。

CREATE DATABASE IF NOT EXISTS `s-pay-mall` DEFAULT CHARACTER SET utf8mb4;
USE `s-pay-mall`;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 's-pay-mall' AND TABLE_NAME = 'pay_order' AND COLUMN_NAME = 'service_package_id') = 0,
    'ALTER TABLE pay_order ADD COLUMN service_package_id varchar(16) NULL COMMENT ''AI服务套餐ID'' AFTER product_id',
    'SELECT ''pay_order.service_package_id exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 's-pay-mall' AND TABLE_NAME = 'pay_order' AND COLUMN_NAME = 'total_quota') = 0,
    'ALTER TABLE pay_order ADD COLUMN total_quota int DEFAULT NULL COMMENT ''大模型调用总额度'' AFTER product_name',
    'SELECT ''pay_order.total_quota exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE pay_order
SET service_package_id = product_id
WHERE service_package_id IS NULL OR service_package_id = '';

CREATE TABLE IF NOT EXISTS subscription_entitlement (
     id bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
     order_id varchar(16) NOT NULL COMMENT '支付商城订单ID',
     user_id varchar(32) NOT NULL COMMENT '用户ID',
     service_package_id varchar(16) NOT NULL COMMENT 'AI服务套餐ID',
     total_quota int NOT NULL COMMENT '大模型调用总额度',
     used_quota int NOT NULL DEFAULT '0' COMMENT '已消耗额度',
     remaining_quota int NOT NULL COMMENT '剩余额度',
     status varchar(16) NOT NULL COMMENT '权益状态；ACTIVE-已开通、REVOKED-已撤销',
     create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     PRIMARY KEY (id),
     UNIQUE KEY uq_order_package (order_id, service_package_id),
     KEY idx_user_package (user_id, service_package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS subscription_fulfillment_task (
     id bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
     order_id varchar(16) NOT NULL COMMENT '支付商城订单ID',
     user_id varchar(32) NOT NULL COMMENT '用户ID',
     service_package_id varchar(16) NOT NULL COMMENT 'AI服务套餐ID',
     total_quota int DEFAULT NULL COMMENT '大模型调用总额度',
     status varchar(16) NOT NULL COMMENT '任务状态；PENDING-待处理、PROCESSING-处理中、RETRY-待重试、SUCCESS-成功、FAILED-失败',
     retry_count int(10) unsigned NOT NULL DEFAULT '0' COMMENT '重试次数',
     fail_reason varchar(512) DEFAULT NULL COMMENT '失败原因',
     next_retry_time datetime DEFAULT NULL COMMENT '下次重试时间',
     create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     PRIMARY KEY (id),
     UNIQUE KEY uq_order_package (order_id, service_package_id),
     KEY idx_status_next_retry_time (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE DATABASE IF NOT EXISTS group_buy_market DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE group_buy_market;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'group_buy_market' AND TABLE_NAME = 'sku' AND COLUMN_NAME = 'total_quota') = 0,
    'ALTER TABLE sku ADD COLUMN total_quota int NOT NULL DEFAULT 0 COMMENT ''大模型调用总额度'' AFTER goods_name',
    'SELECT ''sku.total_quota exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sku
SET goods_name = 'AI 大模型调用额度包 100 万 tokens',
    total_quota = 1000000,
    original_price = 100.00
WHERE goods_id = '9890001';

UPDATE group_buy_activity
SET activity_name = 'AI 大模型调用额度拼团'
WHERE activity_id = 100123;

UPDATE group_buy_discount
SET discount_name = 'AI 套餐拼团立减',
    discount_desc = '大模型调用额度套餐拼团立减 20 元'
WHERE discount_id = '25120207';
