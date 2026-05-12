package top.licodetech.mall.trigger.http;

import com.alibaba.fastjson2.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import top.licodetech.mall.api.IPayService;
import top.licodetech.mall.api.dto.CreatePayRequestDTO;
import top.licodetech.mall.api.dto.NotifyRequestDTO;
import top.licodetech.mall.api.dto.QueryUserOrderListRequestDTO;
import top.licodetech.mall.api.dto.QueryUserOrderListResponseDTO;
import top.licodetech.mall.api.dto.RefundOrderRequestDTO;
import top.licodetech.mall.api.dto.RefundOrderResponseDTO;
import top.licodetech.mall.api.dto.UserOrderItemDTO;
import top.licodetech.mall.api.response.Response;
import top.licodetech.mall.domain.order.model.entity.OrderEntity;
import top.licodetech.mall.domain.order.model.entity.PayOrderEntity;
import top.licodetech.mall.domain.order.model.entity.ShopCartEntity;
import top.licodetech.mall.domain.order.model.valobj.MarketTypeVO;
import top.licodetech.mall.domain.order.model.valobj.OrderStatusVO;
import top.licodetech.mall.domain.order.service.IOrderService;
import top.licodetech.mall.types.common.Constants;
import top.licodetech.mall.types.exception.AppException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/alipay/")
public class AliPayController implements IPayService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${alipay.alipay_public_key}")
    private String alipayPublicKey;

    @Resource
    private IOrderService orderService;

    /**
     * http://localhost:8080/api/v1/alipay/create_pay_order
     * {
     *     "userId": "10001",
     *     "servicePackageId": "9890001"
     * }
     */
    @RequestMapping(value = "create_pay_order", method = RequestMethod.POST)
    @Override
    public Response<String> createPayOrder(@RequestBody CreatePayRequestDTO createPayRequestDTO) {
        try {
            String servicePackageId = StringUtils.defaultIfBlank(createPayRequestDTO.getServicePackageId(), createPayRequestDTO.getProductId());
            log.info("AI服务套餐下单，创建支付单开始 userId:{} servicePackageId:{}", createPayRequestDTO.getUserId(), servicePackageId);
            String userId = createPayRequestDTO.getUserId();
            String productId = servicePackageId;
            String teamId = createPayRequestDTO.getTeamId();
            Integer marketType = createPayRequestDTO.getMarketType();
            // 下单
            PayOrderEntity payOrderEntity = orderService.createOrder(ShopCartEntity.builder()
                    .userId(userId)
                    .productId(productId)
                    .servicePackageId(servicePackageId)
                    .teamId(teamId)
                    .marketTypeVO(MarketTypeVO.valueOf(marketType))
                    .activityId(createPayRequestDTO.getActivityId())
                    .build());

            log.info("AI服务套餐下单，创建支付单完成 userId:{} servicePackageId:{} orderId:{}", userId, servicePackageId, payOrderEntity.getOrderId());
            return Response.<String>builder()
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .data(payOrderEntity.getPayUrl())
                    .build();
        } catch (Exception e) {
            log.error("AI服务套餐下单，创建支付单失败 userId:{} servicePackageId:{}", createPayRequestDTO.getUserId(), createPayRequestDTO.getServicePackageId(), e);
            return Response.<String>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * http://localhost:8080/api/v1/alipay/query_user_order_list
     * {
     *     "userId": "10001",
     *     "lastId": null,
     *     "pageSize": 10
     * }
     */
    @RequestMapping(value = "query_user_order_list", method = RequestMethod.POST)
    @Override
    public Response<QueryUserOrderListResponseDTO> queryUserOrderList(@RequestBody QueryUserOrderListRequestDTO requestDTO) {
        try {
            if (null == requestDTO) {
                throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "请求参数不能为空");
            }
            int pageSize = parsePageSize(requestDTO.getPageSize());
            log.info("用户订单查询开始 userId:{} lastId:{} pageSize:{}", requestDTO.getUserId(), requestDTO.getLastId(), pageSize);

            List<OrderEntity> orderEntityList = orderService.queryUserOrderList(requestDTO.getUserId(), requestDTO.getLastId(), pageSize);
            boolean hasMore = orderEntityList.size() > pageSize;
            if (hasMore) {
                orderEntityList = orderEntityList.subList(0, pageSize);
            }

            List<UserOrderItemDTO> orderList = new ArrayList<>();
            for (OrderEntity orderEntity : orderEntityList) {
                orderList.add(buildUserOrderItemDTO(orderEntity));
            }

            Long lastId = orderList.isEmpty() ? null : orderList.get(orderList.size() - 1).getId();
            QueryUserOrderListResponseDTO responseDTO = QueryUserOrderListResponseDTO.builder()
                    .orderList(orderList)
                    .hasMore(hasMore)
                    .lastId(lastId)
                    .build();

            log.info("用户订单查询完成 userId:{} lastId:{} hasMore:{}", requestDTO.getUserId(), lastId, hasMore);
            return buildSuccessResponse(responseDTO);
        } catch (AppException e) {
            log.warn("用户订单查询失败 requestDTO:{} code:{} info:{}", JSON.toJSONString(requestDTO), e.getCode(), e.getInfo());
            return buildErrorResponse(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("用户订单查询异常 requestDTO:{}", JSON.toJSONString(requestDTO), e);
            return buildErrorResponse(Constants.ResponseCode.UN_ERROR.getCode(), Constants.ResponseCode.UN_ERROR.getInfo());
        }
    }

    /**
     * http://localhost:8080/api/v1/alipay/refund_order
     * {
     *     "userId": "10001",
     *     "orderId": "1234567890123456"
     * }
     */
    @RequestMapping(value = "refund_order", method = RequestMethod.POST)
    @Override
    public Response<RefundOrderResponseDTO> refundOrder(@RequestBody RefundOrderRequestDTO requestDTO) {
        try {
            if (null == requestDTO) {
                throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "请求参数不能为空");
            }
            log.info("用户退单开始 userId:{} orderId:{}", requestDTO.getUserId(), requestDTO.getOrderId());
            OrderEntity orderEntity = orderService.refundOrder(requestDTO.getUserId(), requestDTO.getOrderId());
            RefundOrderResponseDTO responseDTO = RefundOrderResponseDTO.builder()
                    .orderId(orderEntity.getOrderId())
                    .status(orderEntity.getOrderStatusVO().getCode())
                    .statusDesc(orderEntity.getOrderStatusVO().getDesc())
                    .build();
            log.info("用户退单完成 userId:{} orderId:{} status:{}", requestDTO.getUserId(), requestDTO.getOrderId(), responseDTO.getStatus());
            return buildSuccessResponse(responseDTO);
        } catch (AppException e) {
            log.warn("用户退单失败 requestDTO:{} code:{} info:{}", JSON.toJSONString(requestDTO), e.getCode(), e.getInfo());
            return buildErrorResponse(e.getCode(), e.getInfo());
        } catch (Exception e) {
            log.error("用户退单异常 requestDTO:{}", JSON.toJSONString(requestDTO), e);
            return buildErrorResponse(Constants.ResponseCode.UN_ERROR.getCode(), Constants.ResponseCode.UN_ERROR.getInfo());
        }
    }

    /**
     * http://licodetech.top:8080/api/v1/alipay/alipay_notify_url
     */
    @RequestMapping(value = "alipay_notify_url", method = RequestMethod.POST)
    public String payNotify(HttpServletRequest request) throws AlipayApiException, ParseException {
        log.info("支付回调，消息接收 {}", request.getParameter("trade_status"));

        if (!request.getParameter("trade_status").equals("TRADE_SUCCESS")) {
            return "false";
        }

        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            params.put(name, request.getParameter(name));
        }

        String tradeNo = params.get("out_trade_no");
        String gmtPayment = params.get("gmt_payment");
        String alipayTradeNo = params.get("trade_no");

        String sign = params.get("sign");
        String content = AlipaySignature.getSignCheckContentV1(params);
        boolean checkSignature = AlipaySignature.rsa256CheckContent(content, sign, alipayPublicKey, "UTF-8"); // 验证签名
        // 支付宝验签
        if (!checkSignature) {
            return "false";
        }

        // 验签通过
        log.info("支付回调，交易名称: {}", params.get("subject"));
        log.info("支付回调，交易状态: {}", params.get("trade_status"));
        log.info("支付回调，支付宝交易凭证号: {}", params.get("trade_no"));
        log.info("支付回调，商户订单号: {}", params.get("out_trade_no"));
        log.info("支付回调，交易金额: {}", params.get("total_amount"));
        log.info("支付回调，买家在支付宝唯一id: {}", params.get("buyer_id"));
        log.info("支付回调，买家付款时间: {}", params.get("gmt_payment"));
        log.info("支付回调，买家付款金额: {}", params.get("buyer_pay_amount"));
        log.info("支付回调，支付回调，更新订单 {}", tradeNo);

        orderService.changeOrderPaySuccess(tradeNo, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(gmtPayment));

        return "success";
    }

    @RequestMapping(value = "group_buy_notify", method = RequestMethod.POST)
    @Override
    public String groupBuyNotify(@RequestBody NotifyRequestDTO requestDTO) {
        log.info("拼团回调，组队完成，结算开始 {}", JSON.toJSONString(requestDTO));
        try {
            // 营销结算
            orderService.changeOrderMarketSettlement(requestDTO.getOutTradeNoList());
            return "success";
        } catch (Exception e) {
            log.error("拼团回调，组队完成，结算失败 {}", JSON.toJSONString(requestDTO));
            return "error";
        }
    }

    private int parsePageSize(Integer pageSize) {
        int size = null == pageSize ? DEFAULT_PAGE_SIZE : pageSize;
        if (size <= 0) {
            throw new AppException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), "pageSize必须大于0");
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private UserOrderItemDTO buildUserOrderItemDTO(OrderEntity orderEntity) {
        OrderStatusVO orderStatusVO = orderEntity.getOrderStatusVO();
        String status = null == orderStatusVO ? null : orderStatusVO.getCode();
        String statusDesc = null == orderStatusVO ? null : orderStatusVO.getDesc();
        return UserOrderItemDTO.builder()
                .id(orderEntity.getId())
                .orderId(orderEntity.getOrderId())
                .productName(orderEntity.getProductName())
                .totalAmount(orderEntity.getTotalAmount())
                .payAmount(orderEntity.getPayAmount())
                .status(status)
                .statusDesc(statusDesc)
                .createTime(null == orderEntity.getOrderTime() ? null : new SimpleDateFormat(DATE_TIME_PATTERN).format(orderEntity.getOrderTime()))
                .canRefund(OrderStatusVO.canRefund(status))
                .build();
    }

    private <T> Response<T> buildSuccessResponse(T data) {
        return Response.<T>builder()
                .code(Constants.ResponseCode.SUCCESS.getCode())
                .info(Constants.ResponseCode.SUCCESS.getInfo())
                .data(data)
                .build();
    }

    private <T> Response<T> buildErrorResponse(String code, String info) {
        return Response.<T>builder()
                .code(code)
                .info(info)
                .build();
    }

}
