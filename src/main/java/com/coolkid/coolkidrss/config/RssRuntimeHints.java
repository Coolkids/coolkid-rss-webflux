package com.coolkid.coolkidrss.config;

import com.coolkid.coolkidrss.entity.RssDlInfo;
import com.coolkid.coolkidrss.entity.RssDlLog;
import com.coolkid.coolkidrss.entity.RssFeedInfo;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.coolkid.coolkidrss.entity.RssRuleInfo;
import com.coolkid.coolkidrss.model.request.FeedRecordReq;
import com.coolkid.coolkidrss.model.task.DownloadParam;
import com.coolkid.coolkidrss.model.response.RssPatch;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * @author coolk
 * @version 1.0
 * @date 2024/9/26
 */
public class RssRuntimeHints implements RuntimeHintsRegistrar {
    private final BindingReflectionHintsRegistrar registrar = new BindingReflectionHintsRegistrar();

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        ReflectionHints reflectionHints = hints.reflection();
        registrar.registerReflectionHints(reflectionHints, rssSerializableClasses());
        /**
         * 添加其他外部资源，使用下面代码添加
         * hints.resources().registerPattern("application.properties");
         */

    }

    private Type[] rssSerializableClasses() {
        return new Type[]{
                ArrayList.class,
                String.class,
                RssDlInfo.class,
                RssDlLog.class,
                RssFeedInfo.class,
                RssFeedRecord.class,
                RssPatch.class,
                RssRuleInfo.class,
                FeedRecordReq.class,
                DownloadParam.class
        };
    }
}
