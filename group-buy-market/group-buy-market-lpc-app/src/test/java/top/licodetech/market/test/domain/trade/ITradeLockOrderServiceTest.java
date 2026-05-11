package top.licodetech.market.test.domain.trade;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import top.licodetech.market.api.IMarketTradeService;
import top.licodetech.market.api.dto.LockMarketPayOrderRequestDTO;
import top.licodetech.market.api.dto.LockMarketPayOrderResponseDTO;
import top.licodetech.market.api.response.Response;
import top.licodetech.market.domain.activity.model.entity.MarketProductEntity;
import top.licodetech.market.domain.activity.model.entity.TrialBalanceEntity;
import top.licodetech.market.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import top.licodetech.market.domain.activity.service.IIndexGroupBuyMarketService;
import top.licodetech.market.domain.trade.model.entity.MarketPayOrderEntity;
import top.licodetech.market.domain.trade.model.entity.PayActivityEntity;
import top.licodetech.market.domain.trade.model.entity.PayDiscountEntity;
import top.licodetech.market.domain.trade.model.entity.UserEntity;
import top.licodetech.market.domain.trade.service.ITradeLockOrderService;

import javax.annotation.Resource;

/**
 * @author LiPC
 * @description
 * @create 2025-12-25 21:19
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ITradeLockOrderServiceTest {

    @Resource
    private IIndexGroupBuyMarketService indexGroupBuyMarketService;

    @Resource
    private ITradeLockOrderService tradeOrderService;

    @Resource
    private IMarketTradeService marketTradeService;

//    @Test
//    public void test_lockMarketPayOrder() {
//        LockMarketPayOrderRequestDTO lockMarketPayOrderRequestDTO = new LockMarketPayOrderRequestDTO();
//        lockMarketPayOrderRequestDTO.setUserId("lipeicheng");
//        lockMarketPayOrderRequestDTO.setTeamId(null);
//        lockMarketPayOrderRequestDTO.setActivityId(100123L);
//        lockMarketPayOrderRequestDTO.setGoodsId("9890001");
//        lockMarketPayOrderRequestDTO.setSource("s01");
//        lockMarketPayOrderRequestDTO.setChannel("c01");
//        lockMarketPayOrderRequestDTO.setOutTradeNo(RandomStringUtils.randomNumeric(12));
//        Response<LockMarketPayOrderResponseDTO> lockMarketPayOrderResponseDTOResponse = marketTradeService.lockMarketPayOrder(lockMarketPayOrderRequestDTO);
//        log.info("测试结果 req:{} res:{}", JSON.toJSONString(lockMarketPayOrderRequestDTO), JSON.toJSONString(lockMarketPayOrderResponseDTOResponse));
//    }
    @Test
    public void test_lockMarketPayOrder() throws Exception {
        // 入参信息
        Long activityId = 100123L;
        String userId = "lipeicheng";
        String goodsId = "9890001";
        String source = "s01";
        String channel = "c01";
        String outTradeNo = RandomStringUtils.randomNumeric(12);

        // 1. 获取试算优惠，有【activityId】优先使用
        TrialBalanceEntity trialBalanceEntity = indexGroupBuyMarketService.indexMarketTrial(MarketProductEntity.builder()
                .userId(userId)
                .source(source)
                .channel(channel)
                .goodsId(goodsId)
                .activityId(activityId)
                .build());

        GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = trialBalanceEntity.getGroupBuyActivityDiscountVO();

        // 查询 outTradeNo 是否已经存在交易记录
        MarketPayOrderEntity marketPayOrderEntityOld = tradeOrderService.queryNoPayMarketPayOrderByOutTradeNo(userId, outTradeNo);
        if (null != marketPayOrderEntityOld) {
            log.info("测试结果(Old):{}", JSON.toJSONString(marketPayOrderEntityOld));
            return;
        }

        // 2. 锁定，营销预支付订单；商品下单前，预购锁定。
        MarketPayOrderEntity marketPayOrderEntityNew = tradeOrderService.lockMarketPayOrder(
                UserEntity.builder().userId(userId).build(),
                PayActivityEntity.builder()
                        .teamId(null)
                        .activityId(groupBuyActivityDiscountVO.getActivityId())
                        .activityName(groupBuyActivityDiscountVO.getActivityName())
                        .startTime(groupBuyActivityDiscountVO.getStartTime())
                        .endTime(groupBuyActivityDiscountVO.getEndTime())
                        .validTime(groupBuyActivityDiscountVO.getValidTime())
                        .targetCount(groupBuyActivityDiscountVO.getTarget())
                        .build(),
                PayDiscountEntity.builder()
                        .source(source)
                        .channel(channel)
                        .goodsId(goodsId)
                        .goodsName(trialBalanceEntity.getGoodsName())
                        .originalPrice(trialBalanceEntity.getOriginalPrice())
                        .deductionPrice(trialBalanceEntity.getDeductionPrice())
                        .outTradeNo(outTradeNo)
                        .build());

        log.info("测试结果(New):{}",JSON.toJSONString(marketPayOrderEntityNew));
    }

    @Test
    public void test_lockMarketPayOrder_teamId_not_null() {
        LockMarketPayOrderRequestDTO lockMarketPayOrderRequestDTO = new LockMarketPayOrderRequestDTO();
        lockMarketPayOrderRequestDTO.setUserId("xiaoming07");
        lockMarketPayOrderRequestDTO.setTeamId("96942753");
        lockMarketPayOrderRequestDTO.setActivityId(100123L);
        lockMarketPayOrderRequestDTO.setGoodsId("9890001");
        lockMarketPayOrderRequestDTO.setSource("s01");
        lockMarketPayOrderRequestDTO.setChannel("c01");
        lockMarketPayOrderRequestDTO.setOutTradeNo(RandomStringUtils.randomNumeric(12));
        Response<LockMarketPayOrderResponseDTO> lockMarketPayOrderResponseDTOResponse = marketTradeService.lockMarketPayOrder(lockMarketPayOrderRequestDTO);
        log.info("测试结果 req:{} res:{}", JSON.toJSONString(lockMarketPayOrderRequestDTO), JSON.toJSONString(lockMarketPayOrderResponseDTOResponse));
    }

}
