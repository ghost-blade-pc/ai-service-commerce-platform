package top.licodetech.market.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import top.licodetech.market.infrastructure.dao.po.NotifyTask;

/**
 * @author LiPC
 * @description
 * @create 2026-01-14 16:18
 */
@Mapper
public interface INotifyTaskDao {

    void insert(NotifyTask notifyTask);

}
