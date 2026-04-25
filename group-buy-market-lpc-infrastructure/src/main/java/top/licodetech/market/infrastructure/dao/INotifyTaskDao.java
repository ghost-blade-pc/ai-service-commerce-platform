package top.licodetech.market.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import top.licodetech.market.infrastructure.dao.po.NotifyTask;

import java.util.List;

/**
 * @author LiPC
 * @description
 * @create 2026-01-14 16:18
 */
@Mapper
public interface INotifyTaskDao {

    void insert(NotifyTask notifyTask);

    List<NotifyTask> queryUnExecutedNotifyTaskList();

    NotifyTask queryUnExecutedNotifyTaskByTeamId(String teamId);

    int updateNotifyTaskStatusSuccess(NotifyTask notifyTask);

    int updateNotifyTaskStatusError(NotifyTask notifyTask);

    int updateNotifyTaskStatusRetry(NotifyTask notifyTask);

}
