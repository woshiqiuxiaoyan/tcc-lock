package com.shaoxi.framework.lock.exception;

/**
 * @author: QXY
 * @classDescribe:
 * @createTime: 2019/2/22
 * @version: 1.0
 */
public class MyLockException extends RuntimeException {

    public MyLockException(){

    }
    public MyLockException(String msg){
        super(msg);
    }

    public MyLockException(Throwable throwable){
        super(throwable);
    }

}
