package top.licodetech.market.infrastructure.adapter.repository;

import lombok.extern.slf4j.Slf4j;
import top.licodetech.market.infrastructure.dcc.DCCService;
import top.licodetech.market.infrastructure.redis.IRedisService;

import javax.annotation.Resource;
import java.util.function.Supplier;

/**
 * @author LiPC
 * @description 仓储抽象类
 * @create 2026-04-13 15:18
 */
@Slf4j
public abstract class AbstractRepository {

    @Resource
    protected IRedisService redisService;

    @Resource
    protected DCCService dccService;

    /**
     * 通用缓存处理方法
     * 优先从缓存获取，缓存不存在则从数据库获取并写入缓存
     *
     * @param cacheKey      缓存键
     * @param dbFallback    数据库查询函数
     * @param <T>           返回类型
     * @return              查询结果
     */
    protected <T> T getFromCacheOrDb(String cacheKey, Supplier<T> dbFallback) {
        // 判断是否开启缓存
        if (dccService.isCacheOpenSwitch()) {
            // 从缓存中获取
            T cacheResult = redisService.getValue(cacheKey);
            // 缓存存在则直接返回
            if (null != cacheResult) {
                return cacheResult;
            }
            // 缓存不存在则从数据库获取
            T dbResult = dbFallback.get();
            // 数据库查询结果为空则直接返回
            if (null == dbResult) {
                return null;
            }
            // 写入缓存
            redisService.setValue(cacheKey, dbResult);
            return dbResult;
        } else {
            // 缓存未开启，直接从数据库获取
            log.warn("缓存降级 {}", cacheKey);
            return dbFallback.get();
        }
    }

    /**
     * 通用缓存处理方法（带过期时间）
     * 优先从缓存获取，缓存不存在则从数据库获取并写入缓存
     *
     * @param cacheKey      缓存键
     * @param dbFallback    数据库查询函数
     * @param expired       过期时间
     * @param <T>           返回类型
     * @return              查询结果
     */
    protected <T> T getFromCacheOrDb(String cacheKey, Supplier<T> dbFallback, long expired) {
        // 判断是否开启缓存
        if (dccService.isCacheOpenSwitch()) {
            // 从缓存获取
            T cacheResult = redisService.getValue(cacheKey);
            // 缓存存在则直接返回
            if (null != cacheResult) {
                return cacheResult;
            }
            // 缓存不存在则从数据库获取
            T dbResult = dbFallback.get();
            // 数据库查询结果为空则直接返回
            if (null == dbResult) {
                return null;
            }
            // 写入缓存（带过期时间）
            redisService.setValue(cacheKey, dbResult, expired);
            return dbResult;
        } else {
            // 缓存未开启，直接从数据库获取
            log.warn("缓存降级 {}", cacheKey);
            return dbFallback.get();
        }
    }

}
