package com.sengled.media.clust;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.lang.StringUtils;

public class Functions {

    private static final Function<String, byte[]> str2Bytes = new Function<String, byte[]>() {

        @Override
        public byte[] apply(String t) {
            return t.getBytes();
        }
    };

    private static final Function<byte[], String> bytes2Str = new Function<byte[], String>() {

        @Override
        public String apply(byte[] t) {
            return new String(t);
        }
    };


    @SuppressWarnings("rawtypes")
    private static final Predicate notNUll = new Predicate() {
        public boolean test(Object t) {
            return null != t;
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Predicate NULL = new Predicate() {
        public boolean test(Object t) {
            return null == t;
        }
    };

    private static final Predicate notEmpty = new Predicate<String>() {
        public boolean test(String t) {
            return StringUtils.isNotEmpty(t);
        }
    };

    public static Function<String, byte[]> str2Bytes() {
        return str2Bytes;
    }

    public static Function<byte[], String> bytes2Str() {
        return bytes2Str;
    }

    @SuppressWarnings("unchecked")
    public static <T> java.util.function.Predicate<T> notNull() {
        return (Predicate<T>) notNUll;
    }

    @SuppressWarnings("unchecked")
    public static <T> java.util.function.Predicate<T> isNull() {
        return (Predicate<T>) NULL;
    }

    public static java.util.function.Predicate<String> notEmpty() {
        return notEmpty;
    }

    public Map<byte[], byte[]> toBytesMap(Map<String, String> src) {
        HashMap<byte[], byte[]> bytesMap = new HashMap<>();

        src.forEach(new BiConsumer<String, String>() {
            public void accept(String t, String u) {
                bytesMap.put(t.getBytes(), u.getBytes());
            }
        });

        return bytesMap;
    }
}
