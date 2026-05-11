package top.licodetech.market.api;

import top.licodetech.market.api.dto.GoodsMarketRequestDTO;
import top.licodetech.market.api.dto.GoodsMarketResponseDTO;
import top.licodetech.market.api.response.Response;

/**
 * @author LiPC
 * @description
 * @create 2026-03-09 15:22
 */
public interface IMarketIndexService {

    /**
     * 查询拼团营销配置
     *
     * @param goodsMarketRequestDTO 营销商品信息
     * @return 营销配置信息
     */
    Response<GoodsMarketResponseDTO> queryGroupBuyMarketConfig(GoodsMarketRequestDTO goodsMarketRequestDTO);

}
