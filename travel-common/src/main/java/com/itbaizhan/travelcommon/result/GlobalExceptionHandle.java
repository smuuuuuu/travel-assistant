package com.itbaizhan.travelcommon.result;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandle{

    @ExceptionHandler(BusException.class)
    public BaseResult setException(BusException e){
        return new BaseResult(e.getCode(),e.getMsg(),null);
    }
    @ExceptionHandler(Exception.class)
    public BaseResult defaultException(Exception e){
        e.printStackTrace();
        return new BaseResult(CodeEnum.SYSTEM_ERROR.getCode(), CodeEnum.SYSTEM_ERROR.getMessage(), null);
    }
}
