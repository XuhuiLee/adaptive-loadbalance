package com.aliware.tianchi.common;


import org.apache.dubbo.rpc.Invoker;

/**
 * @author: xuemin5
 * @date: Create at 2019-05-25 13:19
 * @description:
 **/
public interface StatusManager {

    void Refresh(String url ,String msg);

    void cache(Invoker invoker);
}
