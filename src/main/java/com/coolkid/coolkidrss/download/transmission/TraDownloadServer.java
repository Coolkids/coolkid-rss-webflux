package com.coolkid.coolkidrss.download.transmission;

import com.coolkid.coolkidrss.aop.CacheExpire;
import com.coolkid.coolkidrss.download.ClientSecret;
import com.coolkid.coolkidrss.download.DownloadClientApi;
import com.coolkid.coolkidrss.util.EasyUtil;
import com.coolkid.coolkidrss.util.OkHttpUtil;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.olegcherednik.jackson_utils.JacksonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@Data
public class TraDownloadServer implements DownloadClientApi {
    private static final String HEADER_AUTH = "X-Transmission-Session-Id";
    private static final String RPC_PATH = "/transmission/rpc";
    private static final MediaType JSON = MediaType.parse("application/json;charset=utf-8");

    @Override
    public String typeName() {
        return "transmission";
    }

    @Override
    public int type() {
        return 2;
    }

    @Override
    @Cacheable(value = "ClientSecretCache", key = "'transmission_'+#webIp", cacheResolver = "redisExpireCacheResolver")
    @CacheExpire(ttl = 5, unit = TimeUnit.MINUTES)
    public ClientSecret getClientSecret(String webIp, String username, String password) {
        String auth = "Basic "+ EasyUtil.base64(username+":"+password);
        URL rpcUrl = getURL(webIp);
        RequestBody requestBody = RequestBody.create("{\"method\": \"session-get\"}", JSON);

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(rpcUrl)
                .header("Authorization", auth)
                .post(requestBody)
                .build();

        Call call = OkHttpUtil.build().newCall(request);

        try(Response execute = call.execute()){
            ClientSecret clientSecret = new ClientSecret();
            clientSecret.setWebIp(webIp);
            clientSecret.setAuthorization(auth);
            if (execute.code() == 409) {
                clientSecret.setSessionId(execute.header(HEADER_AUTH));
            }
            return clientSecret;
        }catch (IOException e){
            log.error(e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override
    public boolean addTorrent(ClientSecret clinet, String savePath, String urls) {
        String authorization = clinet.getAuthorization();
        String webIp = clinet.getWebIp();
        String sessionId = clinet.getSessionId();
        URL rpcUrl = getURL(webIp);
        LinkedHashMap<String, Object> param = Maps.newLinkedHashMap();
        LinkedHashMap<String, Object> payload = Maps.newLinkedHashMap();
        payload.put("filename", urls);
        payload.put("paused", false);
        payload.put("download-dir", savePath);
        param.put("method", "torrent-add");
        param.put("arguments", payload);
        param.put("tag", "");

        RequestBody requestBody = RequestBody.create(JacksonUtils.writeValue(param), JSON);

        okhttp3.Request.Builder requestBuild = new okhttp3.Request.Builder()
                .url(rpcUrl)
                .header("Authorization", authorization);

        if(StringUtils.isNotBlank(sessionId)){
            requestBuild.header(HEADER_AUTH, sessionId);
        }

        okhttp3.Request request = requestBuild.post(requestBody).build();

        Call call = OkHttpUtil.build().newCall(request);
        try (Response execute = call.execute()) {
            return execute.code() == 200;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override
    public boolean addTorrent(ClientSecret clinet, String savePath, String urls, boolean root) {
        return addTorrent(clinet, savePath, urls);
    }

    @SneakyThrows
    private URL getURL(String webIp) {
        URL url = URI.create(webIp).toURL();
        String urlBuilder = url.getProtocol() + "://" +
                            url.getHost() + ":" + url.getPort() +
                            RPC_PATH;
        return URI.create(urlBuilder).toURL();
    }
}
