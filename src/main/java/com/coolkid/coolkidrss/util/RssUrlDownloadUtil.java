package com.coolkid.coolkidrss.util;

import cn.hutool.core.thread.ThreadUtil;
import com.coolkid.coolkidrss.aop.CacheExpire;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RssUrlDownloadUtil {
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
}
