package top.licodetech.mall.infrastructure.gateway;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import top.licodetech.mall.infrastructure.gateway.dto.WeixinQrCodeRequestDTO;
import top.licodetech.mall.infrastructure.gateway.dto.WeixinQrCodeResponseDTO;
import top.licodetech.mall.infrastructure.gateway.dto.WeixinTemplateMessageDTO;
import top.licodetech.mall.infrastructure.gateway.dto.WeixinTokenResponseDTO;

/**
 * 微信API服务 retrofit 2
 * 2025-10-10
 */
public interface IWeixinApiService {

    /**
     * @param grantType 获取access_token填写 client_credential
     * @param appId     账号唯一凭证，即 AppID
     * @param appSecret 账号唯一凭证密钥，即 AppSecret
     * @return 响应结果
     */
    @GET("cgi-bin/token")
    Call<WeixinTokenResponseDTO> getToken(@Query("grant_type") String grantType,
                                          @Query("appid") String appId,
                                          @Query("secret") String appSecret);

    /**
     * 获取凭据 ticket
     *
     * @param accessToken     getToken 获取的token信息
     * @param weixinQrCodeRequestDTO 入参对象
     * @return 应答结果
     */
    @POST("cgi-bin/qrcode/create")
    Call<WeixinQrCodeResponseDTO> createQrCode(@Query("access_token") String accessToken, @Body WeixinQrCodeRequestDTO weixinQrCodeRequestDTO);

    /**
     * 发送微信公众号模板消息
     * 文档：https://mp.weixin.qq.com/debug/cgi-bin/readtmpl?t=tmplmsg/faq_tmpl
     *
     * @param accessToken              getToken 获取的 token 信息
     * @param weixinTemplateMessageDTO 入参对象
     * @return 应答结果
     */
    @POST("cgi-bin/message/template/send")
    Call<Void> sendMessage(@Query("access_token") String accessToken, @Body WeixinTemplateMessageDTO weixinTemplateMessageDTO);
}
