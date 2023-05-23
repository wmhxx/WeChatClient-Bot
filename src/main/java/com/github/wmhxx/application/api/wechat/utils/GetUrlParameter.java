package com.github.wmhxx.application.api.wechat.utils;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class GetUrlParameter {

    public static Map<String, Object> getParameter(String url) {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            final String charset = "utf-8";
            url = URLDecoder.decode(url, charset);
            if (url.indexOf('?') != -1) {
                final String contents = url.substring(url.indexOf('?') + 1);
                String[] keyValues = contents.split("&");
                for (int i = 0; i < keyValues.length; i++) {
                    String key = keyValues[i].substring(0, keyValues[i].indexOf("="));
                    String value = keyValues[i].substring(keyValues[i].indexOf("=") + 1);
                    map.put(key, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 测试
     *
     * @param args
     */
    public static void main(String[] args) {
        String url = "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxnewloginpage?ticket=Adu61PowOyMXLTA03DdadGjj@qrticket_0&uuid=QdB_zXUo2g==&lang=zh_CN&scan=1684298411&fun=new&version=v2&mod=desktop";
        Map<String, Object> map = getParameter(url);
        System.out.println(map);
    }

}
