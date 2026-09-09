package com.coolkid.coolkidrss.util;

import cn.hutool.core.date.DateUtil;
import com.coolkid.coolkidrss.model.RuleFilter;
import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.time.DateUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class EasyUtil {
    private static final String HOUR_STR = "hh点mm分";

    private EasyUtil() {

    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String uuidNoSplit() {
        return uuid().replace("-", "");
    }

    @SneakyThrows
    public static String sha256(String key) {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
        byte[] digestByte = messageDigest.digest(key.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(digestByte);
    }

    @SneakyThrows
    public static String sha256(byte[] bytes) {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA256");
        byte[] digestByte = messageDigest.digest(bytes);
        return Hex.encodeHexString(digestByte);
    }

    public static String base64(String key) {
        byte[] encodeBase64 = Base64.encodeBase64(key.getBytes());
        return new String(encodeBase64);
    }

    public static RuleFilter expressionRulePattern(@NonNull String rule) {
        RuleFilter ruleFilter = new RuleFilter();

        List<String> need = Lists.newArrayList();
        List<String> needor = Lists.newArrayList();
        List<String> notneed = Lists.newArrayList();
        List<String> notneedor = Lists.newArrayList();

        String[] split = rule.split("\\s");
        Arrays.stream(split).forEach(t -> {
            String temp = t;
            if (t.startsWith("-")) {
                temp =  Strings.CS.remove(temp, "-");
                if (t.contains("|")) {
                    notneedor.addAll(Arrays.asList(temp.split("\\|")));
                } else {
                    notneed.add(temp);
                }
            } else {
                if (t.contains("|")) {
                    needor.addAll(Arrays.asList(temp.split("\\|")));
                } else {
                    need.add(temp);
                }
            }
        });
        ruleFilter.setNeed(need);
        ruleFilter.setNeedor(needor);
        ruleFilter.setNotneed(notneed);
        ruleFilter.setNotneedor(notneedor);
        return ruleFilter;
    }

    public static String getNowDateStr() {
        return DateUtil.format(new Date(), "yyyy-MM-dd hh:mm:ss");
    }

    public static String noticeDate(Date date) {
        int off = DateUtil.dayOfYear(date) - DateUtil.dayOfYear(new Date());
        if (DateUtils.isSameDay(new Date(), date)) {
            return "今天" + DateUtil.format(date, HOUR_STR);
        } else if (off == 1) {
            return "明天" + DateUtil.format(date, HOUR_STR);
        } else if (off == 2) {
            return "后天" + DateUtil.format(date, HOUR_STR);
        } else {
            String format = (off > 2 && off <= 7) ? "E hh点mm分" : "dd日E hh点mm分";
            return DateUtil.format(date, format);
        }
    }
}
