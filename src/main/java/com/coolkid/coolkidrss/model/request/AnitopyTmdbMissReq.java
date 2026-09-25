package com.coolkid.coolkidrss.model.request;

import lombok.Data;

import java.io.Serializable;

/** TMDB 未匹配标题查询条件。 */
@Data
public class AnitopyTmdbMissReq implements Serializable {
    private String title;
    private String feedId;
    private int page = 1;
    private int pageSize = 20;
}
