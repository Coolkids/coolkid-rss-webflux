package com.coolkid.coolkidrss.download;

public interface DownloadClientApi {
     String typeName();
     int type();
     ClientSecret getClientSecret(String webIp, String username, String password);
     boolean addTorrent(ClientSecret clinet, String savePath, String urls);
     boolean addTorrent(ClientSecret clinet, String savePath, String urls, boolean root);
}
