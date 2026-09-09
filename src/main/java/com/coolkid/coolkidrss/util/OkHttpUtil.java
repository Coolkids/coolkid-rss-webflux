package com.coolkid.coolkidrss.util;

import com.google.common.base.Suppliers;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class OkHttpUtil {
    private OkHttpUtil(){

    }
    private static final Supplier<OkHttpClient> OK_HTTP_CLIENT_FACTORY = Suppliers.memoize(OkHttpClient::new);

    public static OkHttpClient build(){
        return OK_HTTP_CLIENT_FACTORY.get().newBuilder().retryOnConnectionFailure(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public static OkHttpClient build(int timeout, int callTimeout){
        return OK_HTTP_CLIENT_FACTORY.get().newBuilder().retryOnConnectionFailure(true)
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .callTimeout(callTimeout, TimeUnit.SECONDS)
                .readTimeout(callTimeout, TimeUnit.SECONDS)
                .build();
    }
}
