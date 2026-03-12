package top.licodetech.market.api;

import top.licodetech.market.api.dto.LockMarketPayOrderRequestDTO;
import top.licodetech.market.api.dto.LockMarketPayOrderResponseDTO;
import top.licodetech.market.api.dto.SettlementMarketPayOrderRequestDTO;
import top.licodetech.market.api.dto.SettlementMarketPayOrderResponseDTO;
import top.licodetech.market.api.response.Response;

/**
 * @author LiPC
 * @description
 * @create 2025-12-25 20:25
 */
public interface IMarketTradeService {

    /**
     * 营销锁单
     *
     * @param requestDTO 锁单商品信息
     * @return 锁单结果信息
     */
    Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(LockMarketPayOrderRequestDTO requestDTO);

    /**
     * 营销结算
     *
     * @param requestDTO 结算商品信息
     * @return 结算结果信息
     */
    Response<SettlementMarketPayOrderResponseDTO> settlementMarketPayOrder(SettlementMarketPayOrderRequestDTO requestDTO);

}
