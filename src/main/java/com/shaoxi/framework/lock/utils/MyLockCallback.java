package com.shaoxi.framework.lock.utils;

import com.shaoxi.framework.lock.exception.MyLockException;
/**
 * 处理锁的各种状态
 * @Author qxy
 * @class LockCallback
 * @Date: 2019/2/22 15:29
 * @Descript: 处理锁的各种状态
 */
public interface MyLockCallback<T> {
    /**
     * 获取锁处理方式
     * @author: QXY
     * @createTime: 2019/2/22
     * @version: 1.0
     * @param
     * @return T
     */
    T handleObtainLock();

    /**
     * 没有获得锁处理方式
     * @author: QXY
     * @createTime: 2019/2/22
     * @version: 1.0
     * @param
     * @return T
     */
    T handleNotObtainLock();

    /**
     * 用户处理时发生异常处理回调
     * @author: QXY
     * @createTime: 2019/2/22
     * @version: 1.0
     * @param var1
     * @return T
     */
    T handleException(MyLockException var1) throws MyLockException;
}