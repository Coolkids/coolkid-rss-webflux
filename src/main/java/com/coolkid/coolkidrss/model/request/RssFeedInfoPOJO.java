package com.coolkid.coolkidrss.model.request;

import cn.hutool.core.bean.BeanUtil;
import com.coolkid.coolkidrss.entity.RssFeedInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RssFeedInfoPOJO extends RssFeedInfo {
    public RssFeedInfoPOJO(RssFeedInfo t) {
        BeanUtil.copyProperties(t, this);
    }

    public RssFeedInfoPOJO() {
        super();
    }

    public RssFeedInfo toParent(){
        RssFeedInfo rssFeedInfo = new RssFeedInfo();
        BeanUtil.copyProperties(this, rssFeedInfo);
        return rssFeedInfo;
    }
}
