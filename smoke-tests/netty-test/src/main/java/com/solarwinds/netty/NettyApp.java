package com.solarwinds.netty;

import io.netty.util.NetUtil;

import java.util.concurrent.TimeUnit;

public class NettyApp {
    public static void main(String[] args){
        System.out.printf("Number of interfaces: %d%n",NetUtil.NETWORK_INTERFACES.size());
        try {
            TimeUnit.MINUTES.sleep(1);
        } catch (InterruptedException ignore) {

        }
        System.out.printf("Shutting down%n");
    }
}
