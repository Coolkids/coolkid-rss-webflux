package com.coolkid.coolkidrss.model.request;

import cn.hutool.core.bean.BeanUtil;
import com.coolkid.coolkidrss.entity.RssRuleInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author coolk
 * @version 1.0
 * @date 2024/8/14
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RssRuleInfoPOJO extends RssRuleInfo {

    public RssRuleInfoPOJO(RssRuleInfo t) {
        BeanUtil.copyProperties(t, this);
    }

    public RssRuleInfoPOJO() {
        super();
    }

    public RssRuleInfo toParent(){
        RssRuleInfo rssRuleInfo = new RssRuleInfo();
        BeanUtil.copyProperties(this, rssRuleInfo);
        return rssRuleInfo;
    }

}
