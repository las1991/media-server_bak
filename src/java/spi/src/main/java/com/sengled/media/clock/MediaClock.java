package com.sengled.media.clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 精度为 ms 的直播时钟
 * <p>
 * 在收到视频帧后，可以通过 {@link #tick(long)} 来调整时钟。
 * 为了保证时钟的正常运行，如果 {@link MediaClock#tick(long)} 的增幅太大，则自动选用上一次的增幅。
 * <br/>
 * <p>
 * 同时，考虑到视频直播过程中可能会有丢包而导致视频帧时间跳动很大，如果 {@link MediaClock#tick(long)}
 * 的增幅太大而没有超过系统时间时， 则认为是正常的。
 *
 * @author chenxh
 */
public class MediaClock {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaClock.class);


    private static final int MAX_TICK_STEP = 2000;

    private static final int MIN_TICK_STEP = -1000;

    static {
        LOGGER.info("tick step is between [{}, {}]ms", MIN_TICK_STEP, MAX_TICK_STEP);
    }

    private final Rational timeUnit;
    private final long minTickStep;
    private final long maxTickStep;

    private long currTime;
    private long ticked;

    public MediaClock(Rational unit) {
        this(unit, MIN_TICK_STEP, MAX_TICK_STEP);
    }

    public MediaClock(Rational unit, long minTickStepMillis, long maxTickStepMillis) {
        this.timeUnit = unit;
        this.minTickStep = minTickStepMillis;
        this.maxTickStep = maxTickStepMillis;
        this.currTime = unit.convert(SystemClock.currentTimeMillis() - 1000, Rational.MILLISECONDS);
    }


    public long currentTimeMillis() {
        return timeUnit.toMillis(currTime);
    }

    public long currentTime() {
        return currTime;
    }

    public long getTicked() {
        return ticked;
    }

    public Rational getTimeUnit() {
        return this.timeUnit;
    }

    public void tick(long tick, Rational tickUnit) {
        final long systemTime = SystemClock.currentTimeMillis();
        final long currentTimeMillis = currentTimeMillis();
        final long tickedMillis = tickUnit.toMillis(tick);

        // 合法的时间戳增量
        if (minTickStep < tickedMillis && tickedMillis < maxTickStep
                && currentTimeMillis + tickedMillis < systemTime) {
            this.ticked = this.timeUnit.convert(tick, tickUnit);
            this.currTime += this.ticked;
        }

        // 时间增量有点古怪，使用上一帧的时间增量
        // 并且使用上一帧的增量不会导致时间越界
        else if (ticked > 0 && this.timeUnit.toMillis(currTime + ticked) <= systemTime) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("illegal tick step {}, use {} instead", tickUnit.toMillis(tick), timeUnit.toMillis(ticked));
            }
            this.currTime += ticked;
        }

        // 时间不能越界
        else if (systemTime - currentTimeMillis > 0) {
            this.currTime += timeUnit.convert(systemTime - currentTimeMillis, Rational.MILLISECONDS);

        }

        // 时间戳不变
        else {
            LOGGER.debug("tick ignored");
        }
    }

    /**
     * 调整时钟，但是不会把时间调整到超过当前的系统时间
     *
     * @param adjustMillis 大于 0 的整数，1000 表示需要调整 1s
     * @return
     */
    public boolean adjustMillis(long adjustMillis) {
        final long currTime = currentTimeMillis();
        final long newTime = currTime + adjustMillis;
        final long systemTime = SystemClock.currentTimeMillis();

        // 考虑到视频直播的特性, 只能往前调时间。 所以新的时间要比较大
        if (currTime < newTime && newTime < systemTime) {
            this.currTime = timeUnit.convert(newTime, Rational.MILLISECONDS);

            return true;
        }


        return false;
    }

}
