package com.shaoxi.framework.lock.constant;

import lombok.Getter;

/**
 * @Author qxy
 * @class LockRetryFrequency
 * @Date: 2019/2/22 14:41
 * @Descript: 重试间隔时间（单位毫秒）
 */
@Getter
public enum LockRetryFrequency {
    VERY_QUICK(10),
    QUICK(50),
    NORMAL(100),
    SLOW(500),
    VERY_SLOW(1000);

    private int retrySpan = 100;

    LockRetryFrequency(int rf) {
        this.retrySpan = rf;
    }

}
