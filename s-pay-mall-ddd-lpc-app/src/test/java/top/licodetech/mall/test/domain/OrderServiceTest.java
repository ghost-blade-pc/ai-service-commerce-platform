package top.licodetech.mall.test.domain;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import top.licodetech.mall.domain.order.model.entity.PayOrderEntity;
import top.licodetech.mall.domain.order.model.entity.ShopCartEntity;
import top.licodetech.mall.domain.order.model.valobj.MarketTypeVO;
import top.licodetech.mall.domain.order.service.IOrderService;

import javax.annotation.Resource;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class OrderServiceTest {

    @Resource
    private IOrderService orderService;

    @Test
    public void test_createOrder() throws Exception {
        ShopCartEntity shopCartEntity = new ShopCartEntity();
        shopCartEntity.setUserId("xiaofuge");
        shopCartEntity.setProductId("10001");
        PayOrderEntity payOrderEntity = orderService.createOrder(shopCartEntity);
        log.info("请求参数:{}", JSON.toJSONString(shopCartEntity));
        log.info("测试结果:{}", JSON.toJSONString(payOrderEntity));
    }

    @Test
    public void testCreateOrder() throws Exception {
        ShopCartEntity shopCartEntity = new ShopCartEntity();
        shopCartEntity.setUserId("yanxinyao"); // 每次测试用个新的id就可以，不限制人群的情况下，可以随意编写。
        shopCartEntity.setProductId("9890001");
        shopCartEntity.setTeamId("89821049");
        shopCartEntity.setActivityId(100123L);
        shopCartEntity.setMarketTypeVO(MarketTypeVO.GROUP_BUY_MARKET);

        PayOrderEntity payOrderEntity = orderService.createOrder(shopCartEntity);

        log.info("请求参数:{}", JSON.toJSONString(shopCartEntity));
        log.info("测试结果:{}", JSON.toJSONString(payOrderEntity));
    }

}
