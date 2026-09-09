package com.coolkid.coolkidrss.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RssFeedSortReq implements Serializable {
    private List<RssFeedSortItem> data;

    @Data
    public static class RssFeedSortItem implements Serializable {
        private String feedId;
        private int sortOn;
    }
}
