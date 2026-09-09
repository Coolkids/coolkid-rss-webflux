package com.coolkid.coolkidrss.model.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FailResult<T> extends Result<T>{
    public FailResult(){
        this.message = "error";
        this.code = 500;
    }
}
