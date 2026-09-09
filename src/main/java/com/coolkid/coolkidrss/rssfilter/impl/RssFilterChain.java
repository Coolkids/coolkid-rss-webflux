package com.coolkid.coolkidrss.rssfilter.impl;

import com.coolkid.coolkidrss.rssfilter.RssFilterContext;
import com.coolkid.coolkidrss.rssfilter.RssFilterHandler;

import java.util.Objects;
import java.util.regex.Pattern;

public class RssFilterChain extends RssFilterHandler {
    @Override
    public boolean handleRequest(String ruleParam, int ruleType, String feedTitle) {
        boolean b;
        if (ruleType==0){
            RssFilterContext rssFilterContext = new RssFilterContext();
            b = rssFilterContext.expressionInterpreter(ruleParam, feedTitle);
        }else{
            b = Pattern.matches(ruleParam, feedTitle);
        }
        if(b){
            return true;
        }
        if(Objects.nonNull(next)){
            return next.handleRequest(ruleParam, ruleType, feedTitle);
        }else {
            return false;
        }
    }
}
