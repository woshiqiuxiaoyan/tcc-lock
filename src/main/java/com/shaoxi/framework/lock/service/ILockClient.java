package com.shaoxi.framework.lock.service;

import com.shaoxi.framework.lock.constant.LockRetryFrequency;
import com.shaoxi.framework.lock.exception.MyLockException;
import com.shaoxi.framework.lock.utils.MyLockCallback;

/**
 * @author: QXY
 * @classDescribe:
 * @createTime: 2019/2/22
 * @version: 1.0
 */
public interface ILockClient {
    /**
     * 锁
     * @author: QXY
     * @createTime: 2019/2/22
     * @version: 1.0
     * @param key  需要锁住的key
     * @param timeoutInSecond 获取锁的超时时间（单位：毫秒）
     * @param redisKeyExpireSeconds 持有锁的最长时间（单位：毫秒）
     * @param lockCallback  状态对应处理事件
     * @return T
     */
    <T> T lock(String key,int timeoutInSecond,int redisKeyExpireSeconds,MyLockCallback<T> lockCallback) throws MyLockException;

    /**
     * tcc 等待锁
     * @author: QXY
     * @createTime: 2019/2/22
     * @version: 1.0
     * @param key
     * @param xid
     * @param timeoutInSecond
     * @param redisKeyExpireSeconds
     * @param lockCallback
     * @return T
     */
    <T> T lockTCC(String key,String xid,int timeoutInSecond,int redisKeyExpireSeconds,MyLockCallback<T> lockCallback);
    /**
     * 解锁
     * @author: QXY
     * @createTime: 2019/2/22
     * @version: 1.0
     * @param key
     * @param xid
     * @return void
     */
    void unLockTCC(String key,String xid);
}
