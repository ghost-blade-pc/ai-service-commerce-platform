package top.licodetech.mall.domain.auth.adapter.port;

import java.io.IOException;

public interface ILoginPort {
    String createQrCodeTicket() throws IOException;

    void sendLoginTemplate(String openid) throws IOException;

}
