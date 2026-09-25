package com.coolkid.coolkidrss.service.impl;

import com.coolkid.coolkidrss.service.AnitopyMlService;
import com.coolkid.coolkidrss.util.OkHttpUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** anitopy-ml HTTP 客户端，负责将服务响应转换为现有媒体信息结构。 */
@Slf4j
@Service
public class AnitopyMlServiceImpl implements AnitopyMlService {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final int connectTimeoutSeconds;
    private final int callTimeoutSeconds;

    public AnitopyMlServiceImpl(
            ObjectMapper objectMapper,
            @Value("${coolkidrss.anitopy-ml.base-url:http://127.0.0.1:8000}") String baseUrl,
            @Value("${coolkidrss.anitopy-ml.connect-timeout-seconds:5}") int connectTimeoutSeconds,
            @Value("${coolkidrss.anitopy-ml.call-timeout-seconds:15}") int callTimeoutSeconds) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.connectTimeoutSeconds = Math.max(1, connectTimeoutSeconds);
        this.callTimeoutSeconds = Math.max(1, callTimeoutSeconds);
    }

    @Override
    public Optional<Map<String, Object>> parse(String title) {
        if (StringUtils.isBlank(title)) {
            return Optional.empty();
        }
        try {
            String requestJson = objectMapper.writeValueAsString(Map.of("title", title));
            Request request = new Request.Builder()
                    .url(parseUrl())
                    .header("accept", "application/json")
                    .post(RequestBody.create(requestJson, JSON))
                    .build();
            try (Response response = OkHttpUtil.build(connectTimeoutSeconds, callTimeoutSeconds)
                    .newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("anitopy-ml 解析失败，HTTP 状态码：{}", response.code());
                    return Optional.empty();
                }
                return extracted(objectMapper.readTree(response.body().string()));
            }
        } catch (IOException | RuntimeException e) {
            log.warn("调用 anitopy-ml 解析标题失败：{}", title, e);
            return Optional.empty();
        }
    }

    private HttpUrl parseUrl() {
        HttpUrl url = HttpUrl.parse(baseUrl.replaceAll("/+$", "") + "/v1/parse");
        if (url == null) {
            throw new IllegalStateException("无效的 anitopy-ml 地址：" + baseUrl);
        }
        return url;
    }

    private Optional<Map<String, Object>> extracted(JsonNode response) {
        JsonNode extracted = response.path("result").path("extracted");
        if (!extracted.isObject() || extracted.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> result = objectMapper.convertValue(
                extracted, new TypeReference<LinkedHashMap<String, Object>>() { });
        return result.isEmpty() ? Optional.empty() : Optional.of(result);
    }
}
