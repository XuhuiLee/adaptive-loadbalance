package com.aliware.tianchi.common;

import org.apache.dubbo.rpc.Invoker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: xuemin5
 * @date: Create at 2019-05-25 13:10
 * @description: Invoker状态缓存
 **/
public class InvokersManager<T> implements StatusManager {

    final Map<String,InvokerHolder<T>> invokerCache = new ConcurrentHashMap<>(16);


    @Override
    public void Refresh(String url, String msg) {

    }

    @Override
    public void cache(Invoker invoker) {

    }
}
