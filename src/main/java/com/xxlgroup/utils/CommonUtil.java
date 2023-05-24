package com.xxlgroup.utils;

import cn.hutool.extra.servlet.ServletUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

public class CommonUtil {
    static Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    public static String getIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("x-original-forwarded-for");
        if (StringUtils.isBlank(ipAddress)){
            return ServletUtil.getClientIP(request);
        }
        if (isIpv6Address(ipAddress)){
            return ServletUtil.getClientIP(request);
        }
        return ipAddress;

    }

    public static boolean isIpv6Address(String address) {
        try {
            final InetAddress inetAddress = InetAddress.getByName(address);
            return inetAddress instanceof Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }

//    public static void main(String[] args) {
//        String ip = "2408:8440:a00:354a:2d85:598d:e855:2711";
//        System.out.println(isIpv6Address(ip));
//    }


    private static final String ALL_CHAR_NUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static String getStringNumRandom(int length) {
        //生成随机数字和字母,
        Random random = new Random();
        StringBuilder saltString = new StringBuilder(length);
        for (int i = 1; i <= length; ++i) {

            saltString.append(ALL_CHAR_NUM.charAt(random.nextInt(ALL_CHAR_NUM.length())));
        }
        return saltString.toString();
    }
}
