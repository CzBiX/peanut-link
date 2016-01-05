package com.czbix.peanutlink.network.model;

public class LoginParams {
    public final String utime;
    public final String uuid;
    public final String ucode;
    public final String mac;

    public LoginParams(String utime, String uuid, String ucode, String mac) {
        this.utime = utime;
        this.uuid = uuid;
        this.ucode = ucode;
        this.mac = mac;
    }
}
