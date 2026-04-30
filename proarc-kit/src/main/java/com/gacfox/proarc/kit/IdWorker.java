package com.gacfox.proarc.kit;

import java.util.concurrent.ThreadLocalRandom;

/**
 * SnowFlake算法<br />
 * 输出格式（二进制）为：0 00000000000000000000000000000000000000000 0000000000 000000000000<br />
 * 1位固定位 - 41位时间戳（毫秒） - 10位机器ID - 12位序列，共64位
 */
public class IdWorker {
    /**
     * 开始时间截（41位，固定2015-01-01）
     */
    private static final long TWEPOCH = 1420041600000L;
    /**
     * 机器ID位数（10位）
     */
    private static final long WORKER_ID_BITS = 10L;
    /**
     * 序列ID位数（12位）
     */
    private static final long SEQUENCE_BITS = 12L;
    /**
     * 当前机器ID（8位，即0-255）
     */
    private final long workerId;
    /**
     * 当前毫秒自增内序列
     */
    private volatile long sequence = 0L;
    /**
     * 上次生成ID的时间截
     */
    private volatile long lastTimestamp = -1L;
    /**
     * 当前单例实例
     */
    private static IdWorker instance;

    /**
     * 初始化ID生成器
     *
     * @param workerId 分布式环境唯一机器ID（10位，即0-1023）
     */
    public static void init(long workerId) {
        if (instance != null) {
            throw new IllegalStateException("IdWorker can not be initialized more than once");
        }
        instance = new IdWorker(workerId);
    }

    /**
     * 初始化非分布式安全的ID生成器（分布式环境下有冲突可能）
     */
    public static void initInsecure() {
        if (instance != null) {
            throw new IllegalStateException("IdWorker can not be initialized more than once");
        }
        long workerId = ThreadLocalRandom.current().nextInt(0, 1023 + 1);
        instance = new IdWorker(workerId);
    }

    /**
     * 获取ID生成器实例
     *
     * @return 实例
     */
    public static IdWorker getInstance() {
        if (instance == null) {
            throw new RuntimeException("IdWorker not initialized, init() should be invoked first");
        }
        return instance;
    }

    private IdWorker(long workerId) {
        final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("Worker ID can't be greater than %d or less than 0", MAX_WORKER_ID));
        }

        this.workerId = workerId;
    }

    /**
     * 获得下一个ID
     *
     * @return 唯一ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        // 发生时钟回拨，抛出异常拒绝生成
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards, refusing to generate for %d milliseconds", lastTimestamp - timestamp));
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & (~(-1L << SEQUENCE_BITS));
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;

        return ((timestamp - TWEPOCH) << (SEQUENCE_BITS + WORKER_ID_BITS))
                | (workerId << SEQUENCE_BITS)
                | sequence;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 返回以毫秒为单位的当前时间
     *
     * @return 当前时间戳
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }
}
