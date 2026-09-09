package com.coolkid.coolkidrss.controller;

import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.coolkid.coolkidrss.entity.RssRuleInfo;
import com.coolkid.coolkidrss.model.request.RssRuleInfoPOJO;
import com.coolkid.coolkidrss.model.request.RuleListReq;
import com.coolkid.coolkidrss.model.request.TestRuleReq;
import com.coolkid.coolkidrss.model.response.Result;
import com.coolkid.coolkidrss.model.response.RssRuleFeed;
import com.coolkid.coolkidrss.model.response.SuccessResult;
import com.coolkid.coolkidrss.service.DownloadService;
import com.coolkid.coolkidrss.service.RssFeedRecordService;
import com.coolkid.coolkidrss.service.RssRuleInfoService;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/rule")
@Data
public class RuleController {
    private final DownloadService downloadService;
    private final RssFeedRecordService rssFeedRecordService;
    private final RssRuleInfoService rssRuleInfoService;


    @PostMapping("list")
    public Mono<Result<List<RssRuleInfo>>> list(@RequestBody RuleListReq ruleListReq) {
        return rssRuleInfoService.findByRuleTitleLike(ruleListReq.getKeywords()).collectList().map(SuccessResult::new);
    }

    @GetMapping("item")
    public Mono<Result<List<RssRuleFeed>>> item(String ruleId) {
        return rssRuleInfoService.getRuleFeed(ruleId).collectList().map(SuccessResult::new);
    }

    @PostMapping("testRule")
    public Mono<Result<List<RssFeedRecord>>> testRule(@RequestBody TestRuleReq testRuleReq) {
        return rssFeedRecordService.queryByFilter(testRuleReq).collectList().map(SuccessResult::new);
    }

    @PostMapping("save")
    public Mono<Result<String>> save(@RequestBody RssRuleInfoPOJO pojo) {
        return rssRuleInfoService.save(pojo.toParent()).map(t -> new SuccessResult<>());
    }

    @GetMapping("delete")
    public Mono<Result<String>> delete(String ruleId) {
        return rssRuleInfoService.remove(ruleId).then(Mono.fromCallable(SuccessResult::new));
    }

    @PostMapping("dlold")
    public Mono<Result<String>> downloadOld() {
        return downloadService.download().then(Mono.fromCallable(SuccessResult::new));
    }
}
