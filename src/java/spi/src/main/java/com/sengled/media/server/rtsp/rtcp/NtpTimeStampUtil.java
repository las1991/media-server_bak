package com.sengled.media.server.rtsp.rtcp;

/**
 * @author las
 * @date 18-10-8
 */
public class NtpTimeStampUtil {
    private static final long msb0baseTime = 2085978496000L;
    private static final long msb1baseTime = -2208988800000L;
    private static long[] time = new long[2];

    public NtpTimeStampUtil() {
    }

    public static long getTime(long seconds, long fraction) {
        fraction = Math.round(1000.0D * (double)fraction / 4.294967296E9D);
        long msb = seconds & 2147483648L;
        return msb == 0L ? 2085978496000L + seconds * 1000L + fraction : -2208988800000L + seconds * 1000L + fraction;
    }

    public static long[] toNtpTime(long t) {
        boolean useBase1 = t < 2085978496000L;
        long baseTime;
        if (useBase1) {
            baseTime = t - -2208988800000L;
        } else {
            baseTime = t - 2085978496000L;
        }

        long seconds = baseTime / 1000L;
        long fraction = baseTime % 1000L * 4294967296L / 1000L;
        if (useBase1) {
            seconds |= 2147483648L;
        }

        time[0] = seconds;
        time[1] = fraction;
        return time;
    }
}
