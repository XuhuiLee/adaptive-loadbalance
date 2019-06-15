package com.aliware.tianchi.common.enums;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author: xuemin5
 * @date: Create at 2019-05-25 13:14
 * @description:
 **/
public enum InvokerStatus {
    health("h"),
    block("b"),
    dead("d");


    String val;

    InvokerStatus(String code) {
        this.val = code;
    }
}

