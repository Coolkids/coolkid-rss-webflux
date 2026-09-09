package com.coolkid.coolkidrss.controller;

import com.coolkid.coolkidrss.entity.RssDlInfo;
import com.coolkid.coolkidrss.entity.RssDlLog;
import com.coolkid.coolkidrss.model.request.DownloadRecordReq;
import com.coolkid.coolkidrss.model.request.FeedRecordReq;
import com.coolkid.coolkidrss.model.request.RssDlInfoPOJO;
import com.coolkid.coolkidrss.model.response.Page;
import com.coolkid.coolkidrss.model.response.Result;
import com.coolkid.coolkidrss.model.response.SuccessResult;
import com.coolkid.coolkidrss.service.RssDlInfoService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Data
@RestController
@RequestMapping("/api/dl")
public class DownloadCtl {
    private final RssDlInfoService rssDlInfoService;

    @GetMapping("list")
    public Mono<Result<List<RssDlInfo>>> list(){
        return rssDlInfoService.findAll().collectList().map(SuccessResult::new);
    }

    @PostMapping("save")
    public Mono<Result<String>> save(@RequestBody RssDlInfoPOJO rssDlInfo){
        return rssDlInfoService.save(rssDlInfo.toParent()).then(Mono.fromCallable(SuccessResult::new));
    }

    @GetMapping("delete")
    public Mono<Result<String>> delete(String dlId){
        return rssDlInfoService.remove(dlId).then(Mono.fromCallable(SuccessResult::new));
    }

    @PostMapping("log")
    public Mono<Result<Page<RssDlLog>>> log(@RequestBody FeedRecordReq feedRecordReq){
        return rssDlInfoService.getLogs(feedRecordReq).map(SuccessResult::new);
    }

    @PostMapping("download")
    public Mono<Result<String>> download(@RequestBody DownloadRecordReq downloadRecordReq){
        return rssDlInfoService.download(downloadRecordReq).then(Mono.fromCallable(SuccessResult::new));
    }
}
