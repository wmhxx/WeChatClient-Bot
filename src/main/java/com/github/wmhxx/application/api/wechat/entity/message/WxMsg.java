package com.github.wmhxx.application.api.wechat.entity.message;

/**
 * wx消息常量
 *
 * @author wmhxx
 * @date 2023/05/23
 */
public class WxMsg {

    /**
     * 微信消息换行符
     */
    public static final String LINE = "\n";

    /**
     * 微信群里@人后面的类似空格的字符。不是空格。如“@nickname ”  \u2005 ?？
     */
    public static final String AT_ME_SPACE = " ";

    /**
     * 接收的消息内容的换行。
     */
    public static final String RECEIVE_MSG_LINE = "<br/>";
}
