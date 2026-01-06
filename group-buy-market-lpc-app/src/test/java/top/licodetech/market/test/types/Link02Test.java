package top.licodetech.market.test.types;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import top.licodetech.market.test.types.rule02.factory.Rule02TradeRuleFactory;
import top.licodetech.market.test.types.rule02.logic.XxxResponse;
import top.licodetech.market.types.design.framwork.link.model2.chain.BusinessLinkedList;

import javax.annotation.Resource;

/**
 * @author LiPC
 * @description
 * @create 2025-12-29 10:56
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Link02Test {

    @Resource(name = "demo01")
    private BusinessLinkedList<String, Rule02TradeRuleFactory.DynamicContext, XxxResponse> businessLinkedList01;

    @Resource(name = "demo02")
    private BusinessLinkedList<String, Rule02TradeRuleFactory.DynamicContext, XxxResponse> businessLinkedList02;

    @Test
    public void test_model02_01() throws Exception {
        XxxResponse apply = businessLinkedList01.apply("123", new Rule02TradeRuleFactory.DynamicContext());
        log.info("测试结果:{}", JSON.toJSONString(apply));
    }

    @Test
    public void test_model02_02() throws Exception {
        XxxResponse apply = businessLinkedList02.apply("123", new Rule02TradeRuleFactory.DynamicContext());
        log.info("测试结果:{}", JSON.toJSONString(apply));
    }

}
