package top.licodetech.mall.api.dto;

import lombok.Data;

@Data
public class QueryUserOrderListRequestDTO {

    // 用户ID
    private String userId;
    // 游标分页参数，第一页为空
    private Long lastId;
    // 每页数量
    private Integer pageSize = 10;

}
