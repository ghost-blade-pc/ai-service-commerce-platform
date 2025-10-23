package top.licodetech.mall.api;

import top.licodetech.mall.api.response.Response;

public interface IAuthService {

    Response<String> weixinQrCodeTicket();

    Response<String> checkLogin(String ticket);

}
