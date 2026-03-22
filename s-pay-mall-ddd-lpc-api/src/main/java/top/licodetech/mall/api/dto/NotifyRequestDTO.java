package top.licodetech.mall.api.dto;

import lombok.Data;

import java.util.List;

/**
 * @author LiPC
 * @description  回调请求对象
 * @create 2026-03-21 18:42
 */
@Data
public class NotifyRequestDTO {

    /** 组队ID */
    private String teamId;
    /** 外部单号 */
    private List<String> outTradeNoList;

}
