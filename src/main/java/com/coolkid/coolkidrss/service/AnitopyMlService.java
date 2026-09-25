package com.coolkid.coolkidrss.service;

import java.util.Map;
import java.util.Optional;

/** anitopy-ml 标题解析服务客户端。 */
public interface AnitopyMlService {

    /** 解析媒体标题，返回 anitopy-ml 的 extracted 字段。 */
    Optional<Map<String, Object>> parse(String title);
}
