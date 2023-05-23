package com.github.wmhxx.application.api.wechat.utils;

import com.github.wmhxx.application.api.wechat.entity.message.WXMessage;
import com.github.wmhxx.application.api.wechat.entity.message.WxMsg;

public class MsgUtil {

    /**
     * 引用消息（组装）
     *
     * @param message  消息
     * @param response 响应
     * @return {@link String}
     */
    public static String quote(WXMessage message, String response) {
        StringBuilder builder = new StringBuilder();
        builder.append("「").append(message.fromUser.name).append(" : ").append(message.content.replaceAll(WxMsg.AT_ME_SPACE, " ")).append("」");
        builder.append(WxMsg.LINE);
        builder.append("—————————————");
        builder.append(WxMsg.LINE);
        builder.append(response);
        return builder.toString();
    }
}
