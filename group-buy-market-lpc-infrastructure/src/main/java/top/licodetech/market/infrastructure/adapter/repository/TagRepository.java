package top.licodetech.market.infrastructure.adapter.repository;

import org.redisson.api.RBitSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.security.AbstractAuthenticationAuditListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import top.licodetech.market.domain.tag.adapter.repository.ITagRepository;
import top.licodetech.market.domain.tag.model.entity.CrowdTagsJobEntity;
import top.licodetech.market.infrastructure.dao.ICrowdTagsDao;
import top.licodetech.market.infrastructure.dao.ICrowdTagsDetailDao;
import top.licodetech.market.infrastructure.dao.ICrowdTagsJobDao;
import top.licodetech.market.infrastructure.dao.po.CrowdTags;
import top.licodetech.market.infrastructure.dao.po.CrowdTagsDetail;
import top.licodetech.market.infrastructure.dao.po.CrowdTagsJob;
import top.licodetech.market.infrastructure.redis.IRedisService;

import javax.annotation.Resource;

@Repository
public class TagRepository implements ITagRepository {

    @Resource
    private ICrowdTagsDao crowdTagsDao;
    @Resource
    private ICrowdTagsDetailDao crowdTagsDetailDao;
    @Resource
    private ICrowdTagsJobDao crowdTagsJobDao;

    @Resource
    private IRedisService redisService;

    @Override
    public CrowdTagsJobEntity queryCrowdTagsJobEntity(String tagId, String batchId) {
        CrowdTagsJob crowdTagsJobReq = new CrowdTagsJob();
        crowdTagsJobReq.setTagId(tagId);
        crowdTagsJobReq.setBatchId(batchId);

        CrowdTagsJob crowdTagsJobRes = crowdTagsJobDao.queryCrowdTagsJob(crowdTagsJobReq);
        if (null == crowdTagsJobRes) return null;

        return CrowdTagsJobEntity.builder()
                .tagType(crowdTagsJobRes.getTagType())
                .tagRule(crowdTagsJobRes.getTagRule())
                .statStartTime(crowdTagsJobRes.getStatStartTime())
                .statEndTime(crowdTagsJobRes.getStatEndTime())
                .build();
    }

    @Override
    public void addCrowdTagsUserId(String tagId, String userId) {
        CrowdTagsDetail crowdTagsDetailReq = new CrowdTagsDetail();
        crowdTagsDetailReq.setTagId(tagId);
        crowdTagsDetailReq.setUserId(userId);

        try {
            crowdTagsDetailDao.addCrowdTagsUserId(crowdTagsDetailReq);
        } catch (DuplicateKeyException ignore) {
            // 忽略唯一索引冲突
        }
        // 获取BitSet
        RBitSet bitSet = redisService.getBitSet(tagId);
        bitSet.set(redisService.getIndexFromUserId(userId), true);

    }

    @Override
    public void updateCrowdTagsStatistics(String tagId, int count) {

        CrowdTags crowdTagsReq = new CrowdTags();
        crowdTagsReq.setTagId(tagId);
        crowdTagsReq.setStatistics(count);

        crowdTagsDao.updateCrowdTagsStatistics(crowdTagsReq);

    }

}
