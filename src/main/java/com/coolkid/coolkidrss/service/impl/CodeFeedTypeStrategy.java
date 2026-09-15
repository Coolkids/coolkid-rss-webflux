package com.coolkid.coolkidrss.service.impl;

import com.coolkid.coolkidrss.entity.FeedType;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.coolkid.coolkidrss.service.FeedTypeStrategy;
import com.coolkid.coolkidrss.util.RssUrlDownloadUtil;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 代码 Feed 策略：获取 GitHub 提交的 patch。 */
@Slf4j
@Component
public class CodeFeedTypeStrategy implements FeedTypeStrategy {
    private static final Pattern GITHUB_COMMIT = Pattern.compile(
            "^https?://github\\.com/([^/]+)/([^/]+)/(?:commit|commits)/([0-9a-fA-F]+)(?:[/?#].*)?$");

    private final RssUrlDownloadUtil rssUrlDownloadUtil;

    @Value("${coolkidrss.rss.code.patch.max-bytes:524288}")
    private int patchMaxBytes;

    public CodeFeedTypeStrategy(RssUrlDownloadUtil rssUrlDownloadUtil) {
        this.rssUrlDownloadUtil = rssUrlDownloadUtil;
    }

    @Override
    public Set<FeedType> supportedTypes() {
        return Set.of(FeedType.CODE);
    }

    @Override
    public void process(RssFeedRecord record, SyndEntry entry) {
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
