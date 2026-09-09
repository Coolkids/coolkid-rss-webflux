package com.coolkid.coolkidrss.model.response;

import lombok.Data;

@Data
public abstract class Result<T> {
    protected int code = 200;
    protected String message;
    protected T data;
}
