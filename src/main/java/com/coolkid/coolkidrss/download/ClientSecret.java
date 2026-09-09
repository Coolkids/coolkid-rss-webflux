package com.coolkid.coolkidrss.download;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientSecret implements Serializable {
    private String webIp;
    private String authorization;
    private String sessionId;
}
