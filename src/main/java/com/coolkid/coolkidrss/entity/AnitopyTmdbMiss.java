package com.coolkid.coolkidrss.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/** 记录解析后未匹配到 TMDB 的影视标题，供模型改进使用。 */
@Data
@Document(collection = "anitopy_tmdb_miss")
@CompoundIndex(
        name = "anitopy_tmdb_miss_feed_title_idx",
        def = "{'feed_id': 1, 'record_title': 1}",
        unique = true
)
@CompoundIndex(
        name = "anitopy_tmdb_miss_last_seen_idx",
        def = "{'last_seen_at': -1}"
)
public class AnitopyTmdbMiss implements Serializable {
    @Id
    private String missId;

    @Field("record_id")
    private String recordId;

    @Field("feed_id")
    private String feedId;

    @Field("record_title")
    private String recordTitle;

    /** anitopy-ml 返回的 result.extracted 内容。 */
    @Field("anitopy_result")
    private Map<String, Object> anitopyResult;

    @Field("first_seen_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date firstSeenAt;

    @Field("last_seen_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastSeenAt;

    @Field("seen_count")
    private long seenCount;
}
