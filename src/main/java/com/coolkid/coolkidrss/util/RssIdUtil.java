package com.coolkid.coolkidrss.util;


import cn.hutool.setting.dialect.Props;
import com.google.common.base.Suppliers;

import java.util.Objects;
import java.util.function.Supplier;

public class RssIdUtil {
    private RssIdUtil(){

    }
    private static Supplier<SnowflakeIdGenerator> generatorSupplier;

    private static void init(){
        Props props = new Props("application.properties");
        String profile = props.getStr("spring.profiles.active");
        Props props2 = new Props("application-"+profile+".properties");
        long databaseId = props2.getLong("coolkidrss.databaseId", 1L);
        long nodeId = props2.getLong("coolkidrss.nodeId", 1L);
        generatorSupplier = Suppliers.memoize(() -> new SnowflakeIdGenerator(nodeId, databaseId));
    }

    public static synchronized Long nextId(){
        if(Objects.isNull(generatorSupplier)){
            init();
        }
        return generatorSupplier.get().generateId();
    }

    public static synchronized String nextIdStr(){
        return nextId().toString();
    }
}
