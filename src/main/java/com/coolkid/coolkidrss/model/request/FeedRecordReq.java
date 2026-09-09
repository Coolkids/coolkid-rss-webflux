package com.coolkid.coolkidrss.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class FeedRecordReq implements Serializable {
    private String keywords;
    private String feedId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;
    private boolean unread = false;
    private boolean fav = false;
    private int page = 1;
    private int pageSize = 15;
}
