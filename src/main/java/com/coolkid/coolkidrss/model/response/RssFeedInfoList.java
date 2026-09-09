package com.coolkid.coolkidrss.model.response;

import cn.hutool.core.bean.BeanUtil;
import com.coolkid.coolkidrss.entity.RssFeedInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Sun Lejun
 * @version 1.0
 * @date 2024/4/29
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class RssFeedInfoList extends RssFeedInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int unRead;

    public RssFeedInfoList(RssFeedInfo rssFeedInfo) {
        BeanUtil.copyProperties(rssFeedInfo, this);
    }
}
