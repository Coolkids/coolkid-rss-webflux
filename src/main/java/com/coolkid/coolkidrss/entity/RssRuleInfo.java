package com.coolkid.coolkidrss.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Document("rss_rule_info")
public class RssRuleInfo implements Serializable {
    /**
     * 规则id
     */
    @Id
    private String ruleId;

    /**
     * 标题
     */
    @Field("rule_title")
    @Indexed(name = "rss_rule_info_idx1", direction = IndexDirection.ASCENDING)
    private String ruleTitle;

    /**
     * 规则参数
     */
    @Field("rule_param")
    private String ruleParam;

    /**
     * 类型 0:普通 1:正则
     */
    @Field("rule_type")
    private Integer ruleType = 0;

    /**
     * 保存路径
     */
    @Field("rule_save_path")
    private String ruleSavePath;

    /**
     * 保存参数 0:默认 1:不新建子文件夹
     */
    @Field("rule_save_param")
    private Integer ruleSaveParam = 0;

    /**
     * 下载工具id
     */
    @Field("dl_id")
    private String dlId;

    /**
     * 状态 0:停用 1:启用 -1:删除
     */
    private Integer status = 1;

    /**
     * 时间戳
     */
    private Date ts = new Date();

    /**
     * 规则对应的feed的id
     */
    @Field("feed_id")
    private List<String> feedIds;

}