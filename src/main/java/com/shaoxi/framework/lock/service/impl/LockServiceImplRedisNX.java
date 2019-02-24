package com.shaoxi.framework.lock.service.impl;

import com.shaoxi.framework.lock.constant.LockRetryFrequency;
import com.shaoxi.framework.lock.exception.MyLockException;
import com.shaoxi.framework.lock.service.ILockClient;
import com.shaoxi.framework.lock.utils.MyLockCallback;
import com.shaoxi.framework.redis.client.IRedisClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author: QXY
 * @classDescribe: rediz实现分布式锁
 * @createTime: 2019/2/22
 * @version: 1.0
 */
@Slf4j
@Data
public class LockServiceImplRedisNX implements ILockClient {

    private IRedisClient redisClient;
    private static final String LOCK_NAMESPACE = "LOCK";
    private static final String XID_LOCK_NAMESPACE="XIDLOCK";

    private final int DefaultDbIndex = 0;


    /**
     * 锁
     * @author: QXY
     * @createTime: 2019/2/22
     * @version: 1.0
     * @param key  需要锁住的key
     * @param timeoutInSecond 获取锁的超时时间（单位：毫秒）
     * @param redisKeyExpireSeconds 持有锁的最长时间（单位：毫秒） 持有锁的时间要大于获取锁的时间 ，反之没有 意义了
     * @param lockCallback  状态对应处理事件
     * @return T
     */
    @Override
    public <T> T lock(String key,int timeoutInSecond,int redisKeyExpireSeconds,MyLockCallback<T> lockCallback) throws MyLockException{
        return this.lock(key,LockRetryFrequency.NORMAL,timeoutInSecond,redisKeyExpireSeconds,lockCallback);
    }


    /**
     * 锁
     * @author: QXY
     * @createTime: 2019/2/22
     * @version: 1.0
     * @param key  需要锁住的key
     * @param frequency 重试的频率
     * @param timeoutInSecond 获取锁的超时时间（单位：毫秒）
     * @param redisKeyExpireSeconds 持有锁的最长时间（单位：毫秒）
     * @param lockCallback  状态对应处理事件
     * @return T
     */
    public <T> T lock(String key,LockRetryFrequency frequency,int timeoutInSecond,int redisKeyExpireSeconds,MyLockCallback<T> lockCallback) {
        long curentTime = System.currentTimeMillis();

        /**
         * 设置加锁过期时间
         */
        long expireSecond = curentTime / 1000L + redisKeyExpireSeconds;
        /**
         * 作为值存入锁中(记录这把锁持有最终时限)
         */
        long expireMillisSecond = curentTime + redisKeyExpireSeconds * 1000L;
        /**
         * 获取锁的超时时间的最后时刻
         */
        long timeoutExprieTime = curentTime + (long)(timeoutInSecond * 1000) - (long)frequency.getRetrySpan();
        /*
         * 重试测试
         */
        int retryCount = timeoutInSecond * 1000 / frequency.getRetrySpan();

        for (int i = 0; i < retryCount; ++i) {

            if (this.redisClient.setnx(key,LOCK_NAMESPACE,String.valueOf(expireMillisSecond),DefaultDbIndex).longValue() == 1L) {
                log.debug("obtain the lock: {},  at {} retry",key,i);
                T t;
                try {
                    this.redisClient.expireAt(key,LOCK_NAMESPACE,expireSecond,DefaultDbIndex);
                    return lockCallback.handleObtainLock();
                } catch (Exception e) {
                    MyLockException ie = new MyLockException(e);
                    t = lockCallback.handleException(ie);
                } finally {
                    this.redisClient.del(key,LOCK_NAMESPACE,DefaultDbIndex);
                }
                return t;
            }

            if (System.currentTimeMillis() >= timeoutExprieTime) {
                log.debug("=============超时没有获得锁，双重校验",key);
                break;
            }
            log.debug("============没有获得锁重试",key,i);
            try {
                Thread.sleep((long)frequency.getRetrySpan());
            } catch (InterruptedException e) {
                log.error("Interrupte exception",e);
            }
        }

        String expireSpecifiedInString = this.redisClient.get(key,LOCK_NAMESPACE,null,DefaultDbIndex);
        if (StringUtils.isNotBlank(expireSpecifiedInString)) {
            long expireSpecified = Long.valueOf(expireSpecifiedInString).longValue();
            if (curentTime > expireSpecified) {
                log.warn("===============持有锁超时,删除锁:{} ,{},{}",new Object[] {key,expireSpecified,curentTime});
                this.redisClient.del(key,"LOCK_NAMESPACE",DefaultDbIndex);
            }
        }
        return  lockCallback.handleNotObtainLock();
    }



    /**
     * 锁
     * @author: QXY
     * @createTime: 2019/2/22
     * @version: 1.0
     * @param key  需要锁住的key
     * @param xid tcc的xid
     * @param timeoutInSecond 获取锁的超时时间（单位：毫秒）
     * @param redisKeyExpireSeconds 持有锁的最长时间（单位：毫秒）
     * @param lockCallback  状态对应处理事件
     * @return T
     */
    @Override
    public <T> T lockTCC(String key,String xid,int timeoutInSecond,int redisKeyExpireSeconds,MyLockCallback<T> lockCallback) {
        return this.lockTCC(key,xid,LockRetryFrequency.NORMAL,timeoutInSecond,redisKeyExpireSeconds,lockCallback);
    }
    /**
     * 锁
     * @author: QXY
     * @createTime: 2019/2/22
     * @version: 1.0
     * @param key  需要锁住的key
     * @param xid tcc的xid
     * @param frequency 重试的频率
     * @param timeoutInSecond 获取锁的超时时间（单位：毫秒）
     * @param redisKeyExpireSeconds 持有锁的最长时间（单位：毫秒）
     * @param lockCallback  状态对应处理事件
     * @return T
     */
    public <T> T lockTCC(String key,String xid,LockRetryFrequency frequency,int timeoutInSecond,int redisKeyExpireSeconds,MyLockCallback<T> lockCallback) {
        long curentTime = System.currentTimeMillis();

        /*
         * 设置加锁过期时间
         */
        long expireSecond = curentTime / 1000L + redisKeyExpireSeconds;
        /*
         * 作为值存入锁中(记录这把锁持有最终时限)
         */
        long expireMillisSecond = curentTime + redisKeyExpireSeconds * 1000L;
        /*
         * 获取锁的超时时间的最后时刻
         */
        long timeoutExprieTime = curentTime + (long)(timeoutInSecond * 1000) - (long)frequency.getRetrySpan();
        /*
         * 重试测试
         */
        int retryCount = timeoutInSecond * 1000 / frequency.getRetrySpan();

        String tccKey=xid+key;

        String value= String.valueOf(expireMillisSecond)+","+tccKey;

        for (int i = 0; i < retryCount; ++i) {
            if (this.redisClient.setnx(key,XID_LOCK_NAMESPACE,value,DefaultDbIndex).longValue() == 1L) {
                /*
                 *  tcc 锁
                 */
                    log.debug("obtain the lock: {},  at {} retry",key,i);
                    T t;
                    try {
                        this.redisClient.expireAt(key,XID_LOCK_NAMESPACE,expireSecond,DefaultDbIndex);
                        return lockCallback.handleObtainLock();
                    } catch (Exception e) {
                        /*
                         * 解分布式锁
                         */
                        this.redisClient.del(key,XID_LOCK_NAMESPACE,DefaultDbIndex);
                        MyLockException ie = new MyLockException(e);
                        t = lockCallback.handleException(ie);
                    }
                    return t;
            }

            if (System.currentTimeMillis() >= timeoutExprieTime) {
                log.debug("=============超时没有获得锁，双重校验",key);
                break;
            }
            log.debug("============没有获得锁重试",key,i);
            try {
                Thread.sleep((long)frequency.getRetrySpan());
            } catch (InterruptedException e) {
                log.error("Interrupte exception",e);
            }
        }

        String expireSpecifiedInString = this.redisClient.get(key,XID_LOCK_NAMESPACE,null,DefaultDbIndex);
        if (StringUtils.isNotBlank(expireSpecifiedInString)) {
            long expireSpecified = Long.valueOf(expireSpecifiedInString.split(",")[0]).longValue();
            if (curentTime > expireSpecified) {
                log.warn("===============持有锁超时,删除锁:{} ,{},{}",new Object[] {key,expireSpecified,curentTime});
                this.redisClient.del(key,XID_LOCK_NAMESPACE,DefaultDbIndex);
            }
        }
        return  lockCallback.handleNotObtainLock();
    }


    /**
     * 解tcc锁
     * @author: QXY
     * @createTime: 2019/2/22
     * @version: 1.0
     * @param key
     * @param xid
     * @return void
     */
    @Override
    public void unLockTCC(String key,String xid){
        String tccKey=xid+key;
        String value =   this.redisClient.get(key,XID_LOCK_NAMESPACE,null,DefaultDbIndex);
        if(StringUtils.isNotBlank(value)
                && tccKey.equals(value.split(",")[1])){
            this.redisClient.del(key,XID_LOCK_NAMESPACE,DefaultDbIndex);
        }
    }
}
