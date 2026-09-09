package com.coolkid.coolkidrss.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Date;

@Data
@Document(collection = "rss_dl_log")
public class RssDlLog implements Serializable {
    @Id
    private String logId;

    @Field("record_title")
    private String recordTitle;

    @Field("dl_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date dlDate = new Date();

    @Field("feed_id")
    @Indexed(name = "rss_dl_log_idx")
    private String feedId;

    @Field("feed_name")
    private String feedName;

    @Field("rule_id")
    private String ruleId;

    @Field("dl_id")
    private String dlId;

    @Field("rule_info")
    private String ruleInfo;

    @Field("dl_info")
    private String dlInfo;
}
