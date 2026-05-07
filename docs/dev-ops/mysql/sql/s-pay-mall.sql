# ************************************************************
# Sequel Ace SQL dump
# 版本号： 20050
#
# https://sequel-ace.com/
# https://github.com/Sequel-Ace/Sequel-Ace
#
# 主机: 127.0.0.1 (MySQL 8.0.32)
# 数据库: xfg-dev-tech-alipay-sandbox
# 生成时间: 2023-12-13 12:02:08 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE='NO_AUTO_VALUE_ON_ZERO', SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE database if NOT EXISTS `s-pay-mall` default character set utf8mb4 ;
use `s-pay-mall`;

# 转储表 pay_order
# ------------------------------------------------------------

DROP TABLE IF EXISTS `pay_order`;

CREATE TABLE `pay_order` (
     `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
     `user_id` varchar(32) NOT NULL COMMENT '用户ID',
     `product_id` varchar(16) NOT NULL COMMENT '商品ID',
     `product_name` varchar(64) NOT NULL COMMENT '商品名称',
     `order_id` varchar(16) NOT NULL COMMENT '订单ID',
     `order_time` datetime NOT NULL COMMENT '下单时间',
     `total_amount` decimal(8,2) unsigned DEFAULT NULL COMMENT '订单金额',
     `status` varchar(32) NOT NULL COMMENT '订单状态；CREATE-创建完成、PAY_WAIT-等待支付、PAY_SUCCESS-支付成功、DEAL_DONE-交易完成、CLOSE-订单关单、REFUNDING-退单中、REFUNDED-已退单',
     `pay_url` varchar(2014) DEFAULT NULL COMMENT '支付信息',
     `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
     `market_type` tinyint(1) DEFAULT NULL COMMENT '营销类型；0无营销、1拼团营销',
     `market_deduction_amount` decimal(8,2) DEFAULT NULL COMMENT '营销金额；优惠金额',
     `pay_amount` decimal(8,2) NOT NULL COMMENT '支付金额',
     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     PRIMARY KEY (`id`),
     UNIQUE KEY `uq_order_id` (`order_id`),
     KEY `idx_user_id_product_id` (`user_id`,`product_id`),
     KEY `idx_user_id_id` (`user_id`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4;

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
