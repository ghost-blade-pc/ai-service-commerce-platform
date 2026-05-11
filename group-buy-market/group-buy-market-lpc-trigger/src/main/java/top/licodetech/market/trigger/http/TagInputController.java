package top.licodetech.market.trigger.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.licodetech.market.domain.tag.service.TagService;

import javax.annotation.Resource;

/**
 * @author LiPC
 * @description
 * @create 2026-03-29 20:00
 */
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/gbm/tag")
public class TagInputController {
    @Resource
    private TagService tagService;

    @RequestMapping(value = "tag_push")
    public void tagPush(){
        tagService.execTagBatchJob("RQ_KJHKL98UU78H66554GFDV", "10001");
    }

}
