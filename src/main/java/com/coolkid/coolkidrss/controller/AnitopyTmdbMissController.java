package com.coolkid.coolkidrss.controller;

import com.coolkid.coolkidrss.entity.AnitopyTmdbMiss;
import com.coolkid.coolkidrss.model.request.AnitopyTmdbMissReq;
import com.coolkid.coolkidrss.model.response.Page;
import com.coolkid.coolkidrss.model.response.Result;
import com.coolkid.coolkidrss.model.response.SuccessResult;
import com.coolkid.coolkidrss.service.AnitopyTmdbMissService;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** anitopy-ml 解析后未匹配 TMDB 标题的查询接口。 */
@Data
@RestController
@RequestMapping("/api/anitopyTmdbMiss")
public class AnitopyTmdbMissController {
    private final AnitopyTmdbMissService service;

    @PostMapping("page")
    public Mono<Result<Page<AnitopyTmdbMiss>>> page(@RequestBody AnitopyTmdbMissReq request) {
        return service.page(request).map(SuccessResult::new);
    }
}
