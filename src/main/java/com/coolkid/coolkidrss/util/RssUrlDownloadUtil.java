package com.coolkid.coolkidrss.util;

import cn.hutool.core.thread.ThreadUtil;
import com.coolkid.coolkidrss.aop.CacheExpire;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RssUrlDownloadUtil {
    public record TextDownload(String content, long size, boolean truncated) {
    }

    public String getRssContent(String url, int time){
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url).removeHeader("User-Agent")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36")
                .get().build();

        Call call = OkHttpUtil.build().newCall(request);
        try (Response execute = call.execute()) {
            return execute.body().string();
        }catch (Exception e){
            if(time > 3){
                log.error("链接到{}失败,正在重试过多",url);
                log.error(e.getMessage(), e);
                return "";
            }else {
                ++time;
                ThreadUtil.sleep(1000);
                log.error("链接到{}失败,正在重试当前重试次数:{}",url, time);
                return getRssContent(url, time);
            }
        }
    }

    @Cacheable(value = "RssContentCache", key = "#url", cacheResolver = "redisExpireCacheResolver")
    @CacheExpire(ttl = 10, unit = TimeUnit.MINUTES)
    public String getRssContent(String url){
        return getRssContent(url, 0);
    }

    /** 下载文本并限制最大字节数，适合代码 patch 这类可能很大的响应。 */
    public TextDownload getText(String url, int maxBytes) {
        if (maxBytes < 1) {
            throw new IllegalArgumentException("maxBytes必须大于0");
        }
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .header("Accept", "text/plain, application patch, */*")
                .header("User-Agent", "coolkid-rss")
                .get()
                .build();
        try (Response response = OkHttpUtil.build(5, 15).newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IllegalStateException("HTTP " + response.code());
            }
            try (var input = response.body().byteStream(); var output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                long total = 0;
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read == 0) {
                        continue;
                    }
                    long remaining = maxBytes + 1L - total;
                    if (remaining <= 0) {
                        break;
                    }
                    int written = (int) Math.min(read, remaining);
                    output.write(buffer, 0, written);
                    total += written;
                    if (total > maxBytes) {
                        break;
                    }
                }
                byte[] bytes = output.toByteArray();
                boolean truncated = bytes.length > maxBytes;
                int contentLength = truncated ? maxBytes : bytes.length;
                long size = response.body().contentLength() >= 0
                        ? response.body().contentLength() : bytes.length;
                return new TextDownload(new String(bytes, 0, contentLength, StandardCharsets.UTF_8),
                        size, truncated);
            }
        } catch (Exception e) {
            throw new IllegalStateException("下载文本失败：" + url, e);
        }
    }
}
