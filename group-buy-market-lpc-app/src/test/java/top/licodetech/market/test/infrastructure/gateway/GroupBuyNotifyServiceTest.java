package top.licodetech.market.test.infrastructure.gateway;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import top.licodetech.market.infrastructure.gateway.GroupBuyNotifyService;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author LiPC
 * @description
 * @create 2026-03-04 20:30
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class GroupBuyNotifyServiceTest {

    @Resource
    private GroupBuyNotifyService groupBuyNotifyService;

    @Test
    public void test_notify_api() throws Exception {

        String notifyRequestDTOJSON = "{\"teamId\":\"57199993\",\"outTradeNoList\":\"038426231487,652896391719,619401409195\"}";
        String notifyRequestDTOJSON2 = "{\"teamId\":\"60324852\",\"outTradeNoList\":[\"003155237257\",\"207817395300\",\"280967030945\"]}";

        String response = groupBuyNotifyService.groupBuyNotify("http://127.0.0.1:8091/api/v1/test/group_buy_notify", notifyRequestDTOJSON2);

        log.info("测试结果:{}", response);
    }

    @Test
    public void test() throws IOException {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\"teamId\":\"60324852\",\"outTradeNoList\":\"885508041014\",\"001875492748\",\"812935631542\"}");
        Request request = new Request.Builder()
                .url("http://127.0.0.1:8091/api/v1/test/group_buy_notify")
                .post(body)
                .addHeader("content-type", "application/json")
                .build();

        Response response = client.newCall(request).execute();
        log.info("测试结果:{}", response);
    }


}
