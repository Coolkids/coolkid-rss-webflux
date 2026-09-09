package com.coolkid.coolkidrss.rssfilter.impl;

import com.coolkid.coolkidrss.rssfilter.Expression;
import org.apache.commons.lang3.Strings;

public class OrExpression implements Expression {
    private final String title;
    private String key;
    //反转 -开头为不包含
    private boolean reverse = false;

    public OrExpression(String title, String key) {
        this.title = title;
        this.key = key;
    }

    @Override
    public boolean interpret() {
        if(key.startsWith("-")){
            this.key = Strings.CS.remove(this.key, "-");
            this.reverse = true;
        }
        String[] split = key.split("\\|");
        boolean find = reverse;
        for(String item:split){
            boolean contains = title.toUpperCase().contains(item.toUpperCase());
            if (!reverse && contains) {
                find = true;
            } else if (reverse && contains) {
                find = false;
            }
        }
        return find;
    }
}
