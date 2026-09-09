package com.coolkid.coolkidrss.util;

import lombok.Getter;

@Getter
public enum DLTOOLS {
    QBT("qBittorrent", 1),
    TRANSMISSION("transmission", 2);
    private final String name;
    private final int type;
    DLTOOLS(String name, int type) {
        this.name = name;
        this.type = type;
    }

}
