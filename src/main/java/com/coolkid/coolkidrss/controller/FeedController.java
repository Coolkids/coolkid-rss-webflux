package com.coolkid.coolkidrss.controller;

import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.coolkid.coolkidrss.model.request.FeedRecordReq;
import com.coolkid.coolkidrss.model.request.RssFeedInfoPOJO;
import com.coolkid.coolkidrss.model.request.RssFeedSortReq;
import com.coolkid.coolkidrss.model.response.FailResult;
import com.coolkid.coolkidrss.model.response.Page;
import com.coolkid.coolkidrss.model.response.Result;
import com.coolkid.coolkidrss.model.response.RssFeedInfoList;
import com.coolkid.coolkidrss.model.response.SuccessResult;
import com.coolkid.coolkidrss.service.FeedService;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

@Data
@RestController
@RequestMapping("/api/feed")
public class FeedController {
    private final FeedService feedService;

    @GetMapping("getFeedList")
    public Mono<Result<List<RssFeedInfoList>>> getFeedList(boolean full){
        return feedService.getRessFeedInfoList(full)
                .sort(Comparator.comparingInt(RssFeedInfoList::getSortOn))
                .collectList()
                .map(SuccessResult::new);
    }

    @PostMapping("getFeedRecord")
    public  Mono<Result<Page<RssFeedRecord>>> getFeedRecord(@RequestBody FeedRecordReq feedRecordReq){
        return feedService.getRecord(feedRecordReq).map(SuccessResult::new);
    }

    @GetMapping("readRecord")
    public Mono<Result<String>> readRecord(String recordId){

        return feedService.readRecord(recordId).then(Mono.fromCallable(SuccessResult::new));
    }

    @GetMapping("favRecord")
    public Mono<Result<String>> facRecord(String recordId, Integer fav){
        if(fav != 0 && fav !=1 ){
            return Mono.fromCallable(FailResult::new);
        }

        return feedService.favRecord(recordId, fav).then(Mono.fromCallable(SuccessResult::new));
    }

    @GetMapping("allRead")
    public Mono<Result<String>> allRead(String feedId){
        return feedService.allRead(feedId).then(Mono.fromCallable(SuccessResult::new));
    }

    @GetMapping("delete")
    public Mono<Result<String>> delete(String feedId){
        return feedService.remove(feedId).then(Mono.fromCallable(SuccessResult::new));
    }

    @PostMapping("sorton")
    public Mono<Result<String>> sortOn(@RequestBody RssFeedSortReq rssFeedSortReq) {
        return feedService.sortOn(rssFeedSortReq).then(Mono.fromCallable(SuccessResult::new));
    }

    @PostMapping("save")
    public Mono<Result<String>> save(@RequestBody RssFeedInfoPOJO rssFeedInfo) {
        return feedService.save(rssFeedInfo.toParent()).then(Mono.fromCallable(SuccessResult::new));
    }

    @PostMapping("flush")
    public Mono<Result<String>> flush(@RequestBody RssFeedInfoPOJO rssFeedInfo) {
        return feedService.flush(rssFeedInfo.toParent()).then(Mono.fromCallable(SuccessResult::new));
    }
}
