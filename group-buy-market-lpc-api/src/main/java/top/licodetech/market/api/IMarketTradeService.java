package top.licodetech.market.api;

import top.licodetech.market.api.dto.LockMarketPayOrderRequestDTO;
import top.licodetech.market.api.dto.LockMarketPayOrderResponseDTO;
import top.licodetech.market.api.response.Response;

/**
 * @author LiPC
 * @description
 * @create 2025-12-25 20:25
 */
public interface IMarketTradeService {

    Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(LockMarketPayOrderRequestDTO lockMarketPayOrderRequestDTO);

}
