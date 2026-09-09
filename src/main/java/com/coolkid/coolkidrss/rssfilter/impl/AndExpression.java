package com.coolkid.coolkidrss.rssfilter.impl;

import com.coolkid.coolkidrss.rssfilter.Expression;
import org.apache.commons.lang3.Strings;

public class AndExpression implements Expression  {
    private final String title;
    private String key;
    //反转 -开头为不包含
    private boolean reverse = false;

    public AndExpression(String title, String key) {
        this.title = title;
        this.key = key;
    }

    @Override
    public boolean interpret() {
        if(key.startsWith("-")){
            key = Strings.CS.remove(key, "-");
            this.reverse = true;
        }
        boolean find = reverse;
        boolean contains = title.toUpperCase().contains(key.toUpperCase());
        if (!reverse && contains) {
            find = true;
        } else if (reverse && contains) {
            find = false;
        }
        return find;
    }
}
