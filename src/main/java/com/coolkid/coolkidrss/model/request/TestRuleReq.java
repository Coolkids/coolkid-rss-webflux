package com.coolkid.coolkidrss.model.request;

import lombok.Data;

import java.util.List;

@Data
public class TestRuleReq {
    private String ruleParam;
    private List<String> feedIds;
}
