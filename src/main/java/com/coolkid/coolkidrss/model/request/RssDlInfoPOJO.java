package com.coolkid.coolkidrss.model.request;

import cn.hutool.core.bean.BeanUtil;
import com.coolkid.coolkidrss.entity.RssDlInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RssDlInfoPOJO extends RssDlInfo {
    public RssDlInfoPOJO(RssDlInfo t) {
        BeanUtil.copyProperties(t, this);
    }

    public RssDlInfoPOJO() {
        super();
    }

    public RssDlInfo toParent(){
        RssDlInfo rssDlInfo = new RssDlInfo();
        BeanUtil.copyProperties(this, rssDlInfo);
        return rssDlInfo;
    }
}
