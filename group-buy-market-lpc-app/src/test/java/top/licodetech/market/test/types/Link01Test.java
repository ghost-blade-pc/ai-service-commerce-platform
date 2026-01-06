package top.licodetech.market.test.types;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import top.licodetech.market.test.types.rule01.factory.Rule01TradeRuleFactory;
import top.licodetech.market.types.design.framwork.link.model1.ILogicLink;

import javax.annotation.Resource;

/**
 * @author LiPC
 * @description
 * @create 2025-12-29 10:55
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class Link01Test {

    @Resource
    private Rule01TradeRuleFactory rule01TradeRuleFactory;

    @Test
    public void test_model01_01() throws Exception {
        ILogicLink<String, Rule01TradeRuleFactory.DynamicContext, String> logicLink = rule01TradeRuleFactory.openLogicLink();
        String logic = logicLink.apply("123", new Rule01TradeRuleFactory.DynamicContext());
        log.info("测试结果:{}", JSON.toJSONString(logic));
    }

}
