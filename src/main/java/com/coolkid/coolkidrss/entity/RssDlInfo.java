package com.coolkid.coolkidrss.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.Date;

/**
 * 下载工具表
 */
@Data
@Document("rss_dl_info")
public class RssDlInfo implements Serializable {
    /**
     * 下载工具id
     */
    @Id
    private String dlId;

    /**
     * 下载工具名称
     */
    @Field("dl_name")
    private String dlName;

    /**
     * 下载工具接口地址
     */
    @Field("dl_url")
    private String dlUrl;

    /**
     * 类型 1:qBittorrent 2:transmission
     */
    @Field("dl_type")
    private Integer dlType=1;

    /**
     * 下载工具登录用户 AES加密
     */
    @Field("dl_user")
    private String dlUser;

    /**
     * 下载工具登录密码 AES加密
     */
    @Field("dl_passwd")
    private String dlPasswd;

    /**
     * 状态0:禁用 1:启用
     */
    private Integer status = 1;

    /**
     * 时间戳
     */
    @Field("ts")
    private Date ts = new Date();
}