package com.sengled.cloud.authication.controller;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;

public class TestLoginController {
    
    public static void main(String[] args) {
        System.out.println(LoginController.digest("who-is-your-dady"));
        System.out.println(ByteBufUtil.hexDump(new byte[] {1}));
        System.out.println(PooledByteBufAllocator.DEFAULT.buffer());
    }
}
