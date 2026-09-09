package com.coolkid.coolkidrss.download.qbittorrent;


import lombok.Getter;

@Getter
public enum API_METHODS {
    LOGIN("/api/v2/auth/login"),
    ADD_TORRENT("/api/v2/torrents/add");
    private final String path;
    API_METHODS(String path) {
        this.path = path;
    }
}
