package com.coolkid.coolkidrss.model.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SuccessResult<T> extends Result<T>{
    public SuccessResult() {
        super();
        this.message = "success";
    }
    public SuccessResult(T t) {
        super();
        this.message = "success";
        this.setData(t);
    }
}
