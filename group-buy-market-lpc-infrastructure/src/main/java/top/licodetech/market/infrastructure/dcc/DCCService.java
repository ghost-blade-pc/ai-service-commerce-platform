package top.licodetech.market.infrastructure.dcc;

import org.springframework.stereotype.Service;
import top.licodetech.market.types.annotations.DCCValue;
import top.licodetech.market.types.common.Constants;

import javax.annotation.sql.DataSourceDefinition;
import java.util.Arrays;
import java.util.List;

/**
 * @author LiPC
 */
@Service
public class DCCService {

    /**
     * 降级开关 0关闭、1开启
     */
    @DCCValue("downgradeSwitch:0")
    private String downgradeSwitch;

    @DCCValue("cutRange:100")
    private String cutRange;

    @DCCValue("scBlacklist:s02c02")
    private String scBlacklist;

    @DCCValue("cacheSwitch:0")
    private String cacheOpenSwitch;

    public boolean isDowngradeSwitch() {
        return "1".equals(downgradeSwitch);
    }

    public boolean isCutRange(String userId) {

        // 计算哈希码的绝对值
        int hashCode = Math.abs(userId.hashCode());

        // 获取最后两位
        int lastTwoDigits = hashCode % 100;

        // 判断是否在切量范围
        return lastTwoDigits <= Integer.parseInt(cutRange);

    }

    /**
     * 判断黑名单拦截渠道，true 拦截、false 放行
     */
    public boolean isSCBlackIntercept(String source, String channel) {
        List<String> list = Arrays.asList(scBlacklist.split(Constants.SPLIT));
        return list.contains(source + channel);
    }

    /**
     * 缓存开启开关，0为开启，1为关闭
     */
    public boolean isCacheOpenSwitch(){
        return "0".equals(cacheOpenSwitch);
    }

}
