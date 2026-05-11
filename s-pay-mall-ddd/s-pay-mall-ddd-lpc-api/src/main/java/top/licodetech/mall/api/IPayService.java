package top.licodetech.mall.api;

import top.licodetech.mall.api.dto.CreatePayRequestDTO;
import top.licodetech.mall.api.dto.NotifyRequestDTO;
import top.licodetech.mall.api.dto.QueryUserOrderListRequestDTO;
import top.licodetech.mall.api.dto.QueryUserOrderListResponseDTO;
import top.licodetech.mall.api.dto.RefundOrderRequestDTO;
import top.licodetech.mall.api.dto.RefundOrderResponseDTO;
import top.licodetech.mall.api.response.Response;

public interface IPayService {

    Response<String> createPayOrder(CreatePayRequestDTO createPayRequestDTO);

    Response<QueryUserOrderListResponseDTO> queryUserOrderList(QueryUserOrderListRequestDTO requestDTO);

    Response<RefundOrderResponseDTO> refundOrder(RefundOrderRequestDTO requestDTO);

    String groupBuyNotify(NotifyRequestDTO requestDTO);

//    String payNotify(HttpServletRequest request) throws AlipayApiException;

}
