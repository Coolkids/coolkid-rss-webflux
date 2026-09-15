package com.coolkid.coolkidrss.util;

import com.coolkid.coolkidrss.entity.FeedType;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.rometools.rome.feed.synd.SyndEntry;
import io.github.igorcmoura.anitopy4j.Anitopy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 在 RSS 记录计算唯一值并入库前执行类型专用处理。 */
@Slf4j
@Component
public class FeedTypeProcessor {
    private static final Pattern GITHUB_COMMIT = Pattern.compile(
            "^https?://github\\.com/([^/]+)/([^/]+)/(?:commit|commits)/([0-9a-fA-F]+)(?:[/?#].*)?$");

    private final RssUrlDownloadUtil rssUrlDownloadUtil;

    @Value("${coolkidrss.rss.code.patch.max-bytes:524288}")
    private int patchMaxBytes;

    public FeedTypeProcessor(RssUrlDownloadUtil rssUrlDownloadUtil) {
        this.rssUrlDownloadUtil = rssUrlDownloadUtil;
    }

    public void process(FeedType type, RssFeedRecord record, SyndEntry entry) {
        FeedType actualType = type == null ? FeedType.OTHER : type;
        switch (actualType) {
            case MOVIE -> processMovie(record);
            case CODE -> processCode(record);
            case NEWS, MUSIC, OTHER -> {
                // 保留 RSS 原始内容，后续类型可以在这里扩展。
            }
        }
    }

    private void processMovie(RssFeedRecord record) {
        try {
            Map<String, Object> parsed = Anitopy.parse(record.getRecordTitle());
            if (parsed != null && !parsed.isEmpty()) {
                record.setRecordMediaInfo(new LinkedHashMap<>(parsed));
            }
        } catch (RuntimeException e) {
            // 单条标题解析失败不应导致整个 feed 更新失败。
            log.debug("影视标题解析失败：{}", record.getRecordTitle(), e);
        }
    }

    private void processCode(RssFeedRecord record) {
        String patchUrl = githubPatchUrl(record.getRecordUrl());
        if (patchUrl == null) {
            return;
        }
        record.setRecordPatchUrl(patchUrl);
        try {
            RssUrlDownloadUtil.TextDownload patch = rssUrlDownloadUtil.getText(patchUrl, patchMaxBytes);
            if (StringUtils.isNotBlank(patch.content())) {
                record.setRecordPatch(patch.content());
                record.setRecordPatchSize(patch.size());
                record.setRecordPatchTruncated(patch.truncated());
            }
        } catch (RuntimeException e) {
            // GitHub 临时不可用时仍保存 RSS 记录，原文链接仍可用。
            log.warn("获取代码 patch 失败：{}", patchUrl, e);
        }
    }

    static String githubPatchUrl(String recordUrl) {
        if (StringUtils.isBlank(recordUrl)) {
            return null;
        }
        String normalizedUrl = recordUrl.trim();
        if (normalizedUrl.endsWith(".patch")) {
            return normalizedUrl;
        }
        Matcher matcher = GITHUB_COMMIT.matcher(normalizedUrl);
        if (!matcher.matches()) {
            return null;
        }
        return URI.create("https://github.com/" + matcher.group(1) + "/" + matcher.group(2)
                + "/commit/" + matcher.group(3) + ".patch").toString();
    }
}
