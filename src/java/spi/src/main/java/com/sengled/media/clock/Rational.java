package com.sengled.media.clock;

public class Rational {
    public static final Rational $1 = new Rational(1,1);
    public static final Rational $1_000 = new Rational(1, 1000);
    public static final Rational $8_000 = new Rational(1,  8000);
    public static final Rational $16_000 = new Rational(1, 16000);
    public static final Rational $44_100 = new Rational(1, 44100);
    public static final Rational $90_000 = new Rational(1, 90000);

    public static final Rational MILLISECONDS = $1_000;
    public static final Rational SECONDS = $1;
    
    // 容易造成 LONG 越界，不使用这个常量
    // public static final Rational NANOSECONDS = new Rational(1, TimeUnit.SECONDS.toNanos(1));
    
    final private long num;
    final private long den;

    public Rational(long num, long den) {
        super();
        if (num == 0 || den == 0) {
            throw new IllegalArgumentException(num + "/" + den);
        }

        this.num = num;
        this.den = den;
    }


    public static Rational valueOf(int den) {
        switch (den) {
            case 1000:
                return $1_000;
            case 90000:
                return $90_000;
            case 44100:
                return $44_100;
            case 8000:
                return $8_000;
            case 16000:
                return $16_000;
            case 1:
                return $1;
            case 0:
            default:
                return new Rational(1, den);
        }
    }

    public long convert(int value, Rational unit) {
        if (unit == this) {
            return value;
        }

        return value * unit.num * den / (unit.den * num);
    }

    public long convert(long value, Rational unit) {
        if (unit == this) {
            return value;
        }

        return value * unit.num * den / (unit.den * num);
    }

    public double getClockRate() {
        return ((double) this.den) / this.num;
    }

    public long getDen() {
        return den;
    }
    
    public long getNum() {
        return num;
    }

    @Override
    public String toString() {
        return num + "/" + den;
    }

    public long toMillis(long value) {
        return $1_000.convert(value, this);
    }
}
