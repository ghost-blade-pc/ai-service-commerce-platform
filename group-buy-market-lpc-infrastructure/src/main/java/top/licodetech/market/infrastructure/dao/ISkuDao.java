package top.licodetech.market.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import top.licodetech.market.infrastructure.dao.po.Sku;

@Mapper
public interface ISkuDao {

    Sku querySkuByGoodsId(String goodsId);

}
