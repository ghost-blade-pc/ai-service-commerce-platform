package top.licodetech.mall.infrastructure.gateway;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import top.licodetech.mall.infrastructure.gateway.dto.LockMarketPayOrderRequestDTO;
import top.licodetech.mall.infrastructure.gateway.dto.LockMarketPayOrderResponseDTO;
import top.licodetech.mall.infrastructure.gateway.response.Response;

/**
 * @author LiPC
 * @description
 * @create 2026-03-18 21:07
 */
public interface IGroupBuyMarketService {


    /**
     * 营销锁单
     *
     * @param requestDTO 锁单商品信息
     * @return 锁单结果信息
     */
    @POST("api/v1/gbm/trade/lock_market_pay_order")
    Call<Response<LockMarketPayOrderResponseDTO>> lockMarketPayOrder(@Body LockMarketPayOrderRequestDTO requestDTO);

}
