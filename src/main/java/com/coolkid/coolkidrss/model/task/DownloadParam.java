package com.coolkid.coolkidrss.model.task;


import com.coolkid.coolkidrss.entity.RssDlInfo;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.coolkid.coolkidrss.entity.RssRuleInfo;
import lombok.Data;

import java.io.Serializable;

@Data
public class DownloadParam implements Serializable {
    private RssFeedRecord rssFeedRecord;
    private RssRuleInfo rssRuleInfo;
    private RssDlInfo rssDlInfo;
}
