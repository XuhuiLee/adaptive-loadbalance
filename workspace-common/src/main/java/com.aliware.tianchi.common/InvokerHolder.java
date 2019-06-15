package com.aliware.tianchi.common;

import com.aliware.tianchi.common.enums.InvokerStatus;
import org.apache.dubbo.rpc.Invoker;

/**
 * @author: xuemin5
 * @date: Create at 2019-05-25 13:11
 * @description:
 **/
public class InvokerHolder<T> {

    private Invoker<T> invoker;

    private InvokerStatus status;



    public InvokerHolder(Invoker<T> invoker, InvokerStatus status){
        this.invoker = invoker;
        this.status = status;
    }

    public InvokerHolder(Invoker<T> invoker){
        this.invoker = invoker;
        this.status = InvokerStatus.health;
    }


    public boolean isHealth(){
        return  status == InvokerStatus.health;
    }

    public boolean isBlock(){
        return status == InvokerStatus.block;
    }


    public boolean isDead(){
        return status == InvokerStatus.dead || !invoker.isAvailable();
    }


}
