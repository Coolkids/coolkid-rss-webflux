package com.coolkid.coolkidrss.download.qbittorrent;

import com.coolkid.coolkidrss.aop.CacheExpire;
import com.coolkid.coolkidrss.download.ClientSecret;
import com.coolkid.coolkidrss.download.DownloadClientApi;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.coolkid.coolkidrss.util.OkHttpUtil.build;

@Service
@Slf4j
@Data
public class QbDownloadServer implements DownloadClientApi {

    @Override
    public String typeName() {
        return "qbittorrent";
    }

    @Override
    public int type() {
        return 1;
    }

    @Override
    @Cacheable(value = "ClientSecretCache", key = "'qbittorrent_'+#webIp", cacheResolver = "redisExpireCacheResolver")
    @CacheExpire(ttl = 5, unit = TimeUnit.MINUTES)
    public ClientSecret getClientSecret(String webIp, String username, String password){
        URL loginUrl = getURL(API_METHODS.LOGIN, webIp);
        RequestBody body = new FormBody.Builder()
                .add("username", username)
                .add("password", password).build();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(loginUrl)
                .post(body)
                .build();

        Call call = build().newCall(request);
        log.info("qbittorrent login:{}", loginUrl);
        try (Response execute = call.execute()) {
            List<Cookie> cookies = Cookie.parseAll(request.url(), execute.headers());

            if (CollectionUtils.isNotEmpty(cookies)) {
                AtomicReference<String> sessionId = new AtomicReference<>();
                cookies.stream().filter(t -> t.name().contains("SID")).findFirst().ifPresent(t -> sessionId.set(t.value()));
                if (StringUtils.isNotBlank(sessionId.get())) {
                    ClientSecret clientSecret = new ClientSecret();
                    clientSecret.setWebIp(webIp);
                    clientSecret.setSessionId(sessionId.get());
                    log.info("qbittorrent login: ok");
                    return clientSecret;
                }else {
                    log.info("qbittorrent login: bad cookie, header:{}", execute.headers());
                    throw new IllegalArgumentException("qbittorrent login: bad cookie");
                }
            }else {
                log.info("qbittorrent login: no match header:{}", execute.headers());
                throw new IllegalArgumentException("cookie not found");
            }
        } catch (IOException e) {
            log.error("{}:error", loginUrl);
            log.error(e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }



    @SneakyThrows
    private URL getURL(API_METHODS method, String webIp) {
        URL url = URI.create(webIp).toURL();
        String urlBuilder = url.getProtocol() + "://" +
                            url.getHost() + ":" + url.getPort() +
                            method.getPath();
        return URI.create(urlBuilder).toURL();
    }


    public boolean addTorrent(ClientSecret client, String savePath, String urls) {
        return addTorrent(client, savePath, urls, true);
    }

    public boolean addTorrent(ClientSecret client, String savePath, String urls, boolean root) {
        String cookie = client.getSessionId();
        String webIp = client.getWebIp();
        URL url = getURL(API_METHODS.ADD_TORRENT, webIp);
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("urls", urls)
                .add("savepath", savePath);
        if(!root){
            builder.add("contentLayout", "NoSubfolder");
        }

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .header("Cookie", "SID=" + cookie)
                .post(builder.build())
                .build();

        Call call = build().newCall(request);
        log.info("qbittorrent add torrent:{}", url);
        try (Response execute = call.execute()) {
            log.info("qbittorrent add torrent: ok");
            return execute.code() == 200;
        } catch (IOException e) {
            log.error("url error:{}", url);
            log.error(e.getMessage(), e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
