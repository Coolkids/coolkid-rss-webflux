package com.coolkid.coolkidrss.config;


import com.coolkid.coolkidrss.model.response.FailResult;
import com.coolkid.coolkidrss.model.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobleExceptionHandle {
    @ExceptionHandler(value =Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> exceptionHandler(Exception e){
        log.error("发生异常！原因是:{}",e.getMessage(), e);
        FailResult<String> error = new FailResult<>();
        error.setMessage(e.getMessage());
        return error;
    }
}
