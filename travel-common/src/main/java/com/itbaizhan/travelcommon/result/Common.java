package com.itbaizhan.travelcommon.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public enum Common {
    DEFAULT_AVATAR("默认头像","http://192.168.66.119:9000/demo/1760174498760.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=admin%2F20251011%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20251011T092139Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=17c90ac75be804e14304fdeca733927a3f171a9c69fae6a1f762d7ab7f8616c5")
    ;
    private final String name;
    private final String value;
}
