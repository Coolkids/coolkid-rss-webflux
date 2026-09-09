package com.coolkid.coolkidrss.rssfilter;

import com.coolkid.coolkidrss.rssfilter.impl.AndExpression;
import com.coolkid.coolkidrss.rssfilter.impl.OrExpression;

public class RssFilterContext {
    /**
     * 是否命中规则
     * @param expression
     * @param title
     * @return true 命中 false 未命中
     */
    public boolean expressionInterpreter(String expression, String title) {
        if (expression == null || expression.isBlank()) {
            return true;
        }
        if (title == null) {
            return false;
        }

        boolean result = true;
        for (String ex : expression.trim().split("\\s+")) {
            Expression exp;
            if (ex.contains("|")) {
                exp = new OrExpression(title, ex);
            } else {
                exp = new AndExpression(title, ex);
            }
            result = result && exp.interpret();
        }
        return result;
    }
}
