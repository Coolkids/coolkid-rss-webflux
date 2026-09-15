package com.coolkid.coolkidrss.model.request;

import lombok.Data;

/** 手动重新查询 TMDB 的请求参数。 */
@Data
public class TmdbRefreshReq {
    private String recordId;
    private String name;
}
