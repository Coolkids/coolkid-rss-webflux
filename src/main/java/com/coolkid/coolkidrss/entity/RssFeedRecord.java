package com.coolkid.coolkidrss.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Date;

@Data
@Document("rss_feed_record")
@CompoundIndex(name = "rss_feed_record_idx", def = "{'feed_id': 1, 'record_title': 1, 'record_pubdate': -1, 'record_isdl': 1, 'record_sha256': 1}")
public class RssFeedRecord implements Serializable {
    /**
     * feed记录id
     */
    @Id
    private String recordId;

    /**
     * feed id
     */
    @Field("feed_id")
    private String feedId;

    /**
     * 标题
     */
    @Field("record_title")
    private String recordTitle;

    /**
     * 描述
     */
    @Field("record_description")
    private String recordDescription;

    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Field("record_pubdate")
    private Date recordPubdate;

    /**
     * 接收时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Field("record_rxdate")
    private Date recordRxdate = new Date();

    /**
     * 阅读时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Field("record_readate")
    private Date recordReadate;

    /**
     * feed记录url
     */
    @Field("record_url")
    private String recordUrl;

    /**
     * feed记录附件地址
     */
    @Field("record_dlurl")
    private String recordDlurl;

    /**
     * 附件是否下载 0:未下载 1:已下载
     */
    @Field("record_isdl")
    private Integer recordIsdl = 0;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Field("ts")
    private Date ts = new Date();

    /**
     * 是否是喜爱的 0:不是 1:是
     */
    @Field("record_fav")
    private Integer recordFav = 0;

    /**
     * Bean的json字符串sha256值 用作唯一标识
     */
    @Field("record_sha256")
    @Indexed(name = "rss_feed_record_idx_uni", direction = IndexDirection.ASCENDING,unique = true)
    private String recordSha256;
}