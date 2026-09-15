package com.coolkid.coolkidrss.util;

import com.coolkid.coolkidrss.aop.CacheExpire;
import com.coolkid.coolkidrss.aop.DistributedLock;
import com.coolkid.coolkidrss.entity.FeedType;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jdom2.Content;
import org.jdom2.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Data
public class FeedUtil {
    private final RssUrlDownloadUtil rssUrlDownloadUtil;

    @Value("${coolkidrss.keep.data.month}")
    private int month;

    @SneakyThrows
    public String getFeedTitle(String url) {
        URL rssUrl = URI.create(url).toURL();
        URLConnection urlConnection = rssUrl.openConnection();
        SyndFeed feed = new SyndFeedInput().build(new XmlReader(urlConnection.getInputStream()));
        return feed.getTitle();
    }

    /**
     * 用于返回rss列表 正序排列
     *
     * @param url    rss地址
     * @param feedId feedId
     * @return rss列表
     */
    @SneakyThrows
    @Cacheable(value = "RssFeedRecordCache", key = "#url", cacheResolver = "redisExpireCacheResolver")
    @CacheExpire(ttl = 5, unit = TimeUnit.MINUTES)
    @DistributedLock(name = "'RssGetItems'+#url")
    public List<RssFeedRecord> getItems(String url, String feedId) {
        return getItems(url, feedId, FeedType.OTHER);
    }

    @SneakyThrows
    @Cacheable(value = "RssFeedRecordCache", key = "#url + ':' + #feedType", cacheResolver = "redisExpireCacheResolver")
    @CacheExpire(ttl = 5, unit = TimeUnit.MINUTES)
    @DistributedLock(name = "'RssGetItems'+#url")
    public List<RssFeedRecord> getItems(String url, String feedId, FeedType feedType) {
        log.info("开始获取数据:{}", url);
        String rssContent = rssUrlDownloadUtil.getRssContent(url);

        ArrayList<RssFeedRecord> result = new ArrayList<>();
        if (StringUtils.isBlank(rssContent)) {
            throw new IllegalArgumentException("获取RSS失败:" + url);
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(rssContent.getBytes())) {
            SyndFeed feed = new SyndFeedInput().build(new XmlReader(bis));
            List<SyndEntry> entries = feed.getEntries();
            if (CollectionUtils.isEmpty(entries)) {
                return result;
            }
            //此处要从后向前读取数据 因为rss都是倒序排列
            for (int i = entries.size() - 1; i >= 0; i--) {
                SyndEntry entry = entries.get(i);
                RssFeedRecord rssFeedRecord = new RssFeedRecord();
                rssFeedRecord.setRecordId(RssIdUtil.nextIdStr());
                rssFeedRecord.setFeedId(feedId);
                rssFeedRecord.setRecordTitle(entry.getTitle().trim());
                rssFeedRecord.setRecordUrl(entry.getLink());
                rssFeedRecord.setTs(new Date());
                rssFeedRecord.setRecordFav(0);
                rssFeedRecord.setRecordIsdl(0);
                SyndContent description = entry.getDescription();
                List<SyndContent> contents = entry.getContents();

                if (Objects.nonNull(description)) {
                    rssFeedRecord.setRecordDescription(description.getValue());
                }
                if (CollectionUtils.isNotEmpty(contents)
                        && Objects.nonNull(contents.getFirst())
                        && StringUtils.isNotBlank(contents.getFirst().getValue())) {
                    rssFeedRecord.setRecordDescription(contents.getFirst().getValue());
                }
                rssFeedRecord.setRecordRxdate(new Date());
                List<SyndEnclosure> enclosures = entry.getEnclosures();
                if (CollectionUtils.isNotEmpty(enclosures)) {
                    SyndEnclosure syndEnclosure = enclosures.getFirst();
                    rssFeedRecord.setRecordDlurl(syndEnclosure.getUrl());
                }
                setFeedDate(rssFeedRecord, entry, feed);
                rssFeedRecord.setRecordSha256(calSha256(rssFeedRecord));
                Date lastDate = DateUtils.addMonths(new Date(), -month);
                if (rssFeedRecord.getRecordPubdate().after(lastDate)) {
                    result.add(rssFeedRecord);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IllegalArgumentException("获取rss失败" + url, e);
        }
        return result;
    }

    private void setFeedDate(RssFeedRecord rssFeedRecord, SyndEntry entry, SyndFeed feed) {
        Date date;
        if (feed.getTitle().contains("Mikan Project")) {
            List<Element> foreignMarkup = entry.getForeignMarkup();
            Element element = foreignMarkup.getFirst();
            Content content = element.getContent(2);
            String value = content.getValue();
            // 定义日期时间格式
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            // 解析字符串为 LocalDateTime 对象
            LocalDateTime localDateTime = LocalDateTime.parse(value, formatter);
            // 转换为 Date 对象（Date对象不包含时区信息）
            date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        } else if (Objects.nonNull(entry.getPublishedDate())) {
            date = entry.getPublishedDate();
        } else if (Objects.nonNull(entry.getUpdatedDate())) {
            date = entry.getUpdatedDate();
        } else {
            date = new Date();
        }
        rssFeedRecord.setRecordPubdate(date);
    }

    private String calSha256(RssFeedRecord rssFeedRecord) {
        String stringBuilder = rssFeedRecord.getFeedId() +
                               rssFeedRecord.getRecordTitle() +
                               rssFeedRecord.getRecordDescription() +
                               rssFeedRecord.getRecordUrl() +
                               rssFeedRecord.getRecordDlurl();
        return EasyUtil.sha256(stringBuilder);
    }
}
