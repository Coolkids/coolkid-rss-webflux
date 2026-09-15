package com.coolkid.coolkidrss.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Date;

/**
 * feed表
 */
@Data
@Document(collection = "rss_feed_info")
public class RssFeedInfo implements Serializable {

    @Id
    private String feedId;

    /**
     * feed名称
     */
    @Field("feed_name")
    private String feedName;

    /**
     * feed订阅地址
     */
    @Field("feed_url")
    @Indexed(name = "rss_feed_info_idx_uni", unique = true)
    private String feedUrl;

    /** 内容类型：影视、新闻、代码、音乐、其他。 */
    @Field("feed_type")
    private FeedType feedType = FeedType.OTHER;

    /**
     * 上次更新时间
     */
    @Field("feed_last_update")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date feedLastUpdate = new Date();

    /**
     * 下次更新时间
     */
    @Field("feed_next_update")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date feedNextUpdate;

    /**
     * 更新周期
     */
    @Field("feed_crontab")
    private Integer feedCrontab = 30;

    /**
     * 状态0:停用 1:启用 -1:删除
     */
    @Indexed
    private Integer status = 1;

    /**
     * feed排序值，从1开始，值越小越靠前
     */
    @Field("sort_on")
    private int sortOn;

    /**
     * 时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date ts = new Date();
}
