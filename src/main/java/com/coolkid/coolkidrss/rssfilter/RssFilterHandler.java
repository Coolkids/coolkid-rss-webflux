package com.coolkid.coolkidrss.rssfilter;

import lombok.Setter;

@Setter
public abstract class RssFilterHandler {
    protected RssFilterHandler next;

    public abstract boolean handleRequest(String ruleParam, int ruleType, String feedTitle);
}
