package top.licodetech.mall.api;

import top.licodetech.mall.api.dto.CreatePayRequestDTO;
import top.licodetech.mall.api.dto.NotifyRequestDTO;
import top.licodetech.mall.api.response.Response;

public interface IPayService {

    Response<String> createPayOrder(CreatePayRequestDTO createPayRequestDTO);

    String groupBuyNotify(NotifyRequestDTO requestDTO);

//    String payNotify(HttpServletRequest request) throws AlipayApiException;

}
