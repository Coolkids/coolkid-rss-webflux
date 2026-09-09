package com.coolkid.coolkidrss.model;

import com.google.common.collect.Lists;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RuleFilter implements Serializable {
    private List<String> need = Lists.newArrayList();
    private List<String> needor = Lists.newArrayList();
    private List<String> notneed = Lists.newArrayList();
    private List<String> notneedor = Lists.newArrayList();
    private String regex;
    private List<Long> feedIds;
    private Integer isdl;
}
