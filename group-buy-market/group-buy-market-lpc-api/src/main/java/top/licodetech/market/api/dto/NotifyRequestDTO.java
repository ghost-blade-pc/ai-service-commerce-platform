package top.licodetech.market.api.dto;

import lombok.Data;

import java.util.List;

/**
 * @author LiPC
 * @description
 * @create 2026-03-04 18:30
 */
@Data
public class NotifyRequestDTO {

    /** 组队ID */
    private String teamId;
    /** 外部单号 */
    private List<String> outTradeNoList;

}
