package com.github.wmhxx.application.api.wechat;

import com.github.wmhxx.application.api.wechat.entity.contact.WXContact;
import com.github.wmhxx.application.api.wechat.entity.message.WXMessage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class WeChatListener {

    /**
     * 获取到用户登录的二维码
     *
     * @param qrCode 用户登录二维码的url
     */
    public abstract void onQRCode(@Nonnull WeChatClient client, @Nonnull String qrCode);

    /**
     * 获取用户头像，base64编码
     *
     * @param base64Avatar base64编码的用户头像
     */
    public void onAvatar(@Nonnull WeChatClient client, @Nonnull String base64Avatar) {
    }

    /**
     * 模拟网页微信客户端异常退出
     *
     * @param reason 错误原因
     */
    public void onFailure(@Nonnull WeChatClient client, @Nonnull String reason) {
        client.dump();
    }

    /**
     * 用户登录并初始化成功
     */
    public void onLogin(@Nonnull WeChatClient client) {
    }

    /**
     * 用户获取到消息
     *
     * @param message 用户获取到的消息
     */
    public void onMessage(@Nonnull WeChatClient client, @Nonnull WXMessage message) {
    }

    /**
     * 用户联系人变化
     *
     * @param client     微信客户端
     * @param oldContact 旧联系人，新增联系人时为null
     * @param newContact 新联系人，删除联系人时为null
     */
    public void onContact(@Nonnull WeChatClient client, @Nullable WXContact oldContact, @Nullable WXContact newContact) {
    }

    /**
     * 模拟网页微信客户端正常退出
     */
    public void onLogout(@Nonnull WeChatClient client) {
        client.dump();
    }
}
