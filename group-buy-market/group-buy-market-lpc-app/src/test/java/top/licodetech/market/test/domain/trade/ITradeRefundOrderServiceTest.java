package top.licodetech.market.test.domain.trade;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import top.licodetech.market.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import top.licodetech.market.domain.trade.model.entity.TradeRefundBehaviorEntity;
import top.licodetech.market.domain.trade.model.entity.TradeRefundCommandEntity;
import top.licodetech.market.domain.trade.service.ITradeRefundOrderService;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 逆向流程单测
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/12 09:07
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ITradeRefundOrderServiceTest {

    @Resource
    private ITradeRefundOrderService tradeRefundOrderService;

    @Test
    public void test_refundOrder() throws Exception {
        TradeRefundCommandEntity tradeRefundCommandEntity = TradeRefundCommandEntity.builder()
                .userId("xfg04")
                .outTradeNo("727869517356")
                .source("s01")
                .channel("c01")
                .build();

        TradeRefundBehaviorEntity tradeRefundBehaviorEntity = tradeRefundOrderService.refundOrder(tradeRefundCommandEntity);

        log.info("请求参数:{}", JSON.toJSONString(tradeRefundCommandEntity));
        log.info("测试结果:{}", JSON.toJSONString(tradeRefundBehaviorEntity));

        // 暂停，等待MQ消息。处理完后，手动关闭程序
        new CountDownLatch(1).await();
    }

    @Test
    // todo 可以进行一个优化，只要队伍内没有合法单就整个队伍都删掉，可以不删除信息，但是要标注队伍解散（合法单，即还在规定时间内的未支付锁单以及已支付且没退款订单）
    // todo 同时要删除Redis中相关队伍的库存key
    public void test_queryTimeoutUnpaidOrderList2Refund() throws Exception {
        List<UserGroupBuyOrderDetailEntity> timeoutOrderList = tradeRefundOrderService.queryTimeoutUnpaidOrderList();

        log.info("查询超时未支付订单列表，数量：{}", timeoutOrderList != null ? timeoutOrderList.size() : 0);

        if (timeoutOrderList != null && !timeoutOrderList.isEmpty()) {
            for (UserGroupBuyOrderDetailEntity orderDetail : timeoutOrderList) {
                log.info("超时订单详情：用户ID={}, 团队ID={}, 活动ID={}, 外部交易单号={}, 有效开始时间={}, 有效结束时间={}",
                        orderDetail.getUserId(),
                        orderDetail.getTeamId(),
                        orderDetail.getActivityId(),
                        orderDetail.getOutTradeNo(),
                        orderDetail.getValidStartTime(),
                        orderDetail.getValidEndTime());

                TradeRefundCommandEntity tradeRefundCommandEntity = TradeRefundCommandEntity.builder()
                        .userId(orderDetail.getUserId())
                        .outTradeNo(orderDetail.getOutTradeNo())
                        .source(orderDetail.getSource())
                        .channel(orderDetail.getChannel())
                        .build();

                TradeRefundBehaviorEntity tradeRefundBehaviorEntity = tradeRefundOrderService.refundOrder(tradeRefundCommandEntity);

                log.info("请求参数(job):{}", JSON.toJSONString(tradeRefundCommandEntity));
                log.info("测试结果(job):{}", JSON.toJSONString(tradeRefundBehaviorEntity));
            }
        } else {
            log.info("当前没有超时未支付订单");
        }

        // 暂停，等待MQ消息。处理完后，手动关闭程序
        new CountDownLatch(1).await();
    }

}
