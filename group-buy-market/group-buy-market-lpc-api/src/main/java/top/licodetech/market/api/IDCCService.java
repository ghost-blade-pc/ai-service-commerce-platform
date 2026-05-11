package top.licodetech.market.api;

import top.licodetech.market.api.response.Response;

/**
 * @author LiPC
 */
public interface IDCCService {

    Response<Boolean> updateConfig(String key, String value);

}
