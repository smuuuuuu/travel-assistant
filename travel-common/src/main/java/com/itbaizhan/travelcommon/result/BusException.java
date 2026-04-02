package com.itbaizhan.travelcommon.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusException extends RuntimeException {
    private int code;
    private String msg;

    public BusException(CodeEnum codeEnum){
        this.code = codeEnum.getCode();
        this.msg = codeEnum.getMessage();
    }
}
