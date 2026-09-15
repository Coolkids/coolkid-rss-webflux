package com.coolkid.coolkidrss.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** RSS 内容类型，用于选择入库前的专用处理器。 */
public enum FeedType {
    MOVIE("影视"),
    NEWS("新闻"),
    CODE("代码"),
    MUSIC("音乐"),
    OTHER("其他");

    private final String label;

    FeedType(String label) {
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static FeedType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        for (FeedType type : values()) {
            if (type.name().equalsIgnoreCase(value) || type.label.equals(value)) {
                return type;
            }
        }
        return OTHER;
    }
}
