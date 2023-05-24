package com.github.wmhxx.application;

import cn.hutool.json.JSONUtil;
import com.github.wmhxx.application.api.wechat.WeChatClient;
import com.github.wmhxx.application.api.wechat.WeChatListener;
import com.github.wmhxx.application.api.wechat.entity.contact.WXContact;
import com.github.wmhxx.application.api.wechat.entity.message.WXMessage;
import com.github.wmhxx.application.api.wechat.service.StrategyServiceImpl;
import com.github.wmhxx.application.api.wechat.utils.QRCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;

@Slf4j
@Component
public class ClientRunApplication implements ApplicationRunner {

    @Resource
    private StrategyServiceImpl strategyService;

    /**
     * 新建一个微信监听器
     */
    public WeChatListener LISTENER = new WeChatListener() {

        @Override
        public void onQRCode(@Nonnull WeChatClient client, @Nonnull String qrCode) {
            log.info("onQRCode：{}", qrCode);
            log.info(QRCodeUtil.getQr(qrCode.replace("qrcode", "l")));
        }

        @Override
        public void onLogin(@Nonnull WeChatClient client) {
            log.info("onLogin：您有{}名好友、活跃微信群{}个", client.userFriends().size(), client.userGroups().size());
            log.info("onLogin：好友信息:{}", JSONUtil.toJsonStr(client.userFriends()));
            log.info("onLogin：群组信息:{}", JSONUtil.toJsonStr(client.userGroups()));
        }

        @Override
        public void onMessage(@Nonnull WeChatClient client, @Nonnull WXMessage message) {
            //log.info("获取到消息：" + JSONUtil.toJsonStr(message));
            strategyService.getClient(client, message);
        }

        @Override
        public void onContact(@Nonnull WeChatClient client, @Nullable WXContact oldContact, @Nullable WXContact newContact) {
            log.info("检测到联系人变更:旧联系人名称：{}:新联系人名称：{}", oldContact != null ? oldContact.name : null, (newContact == null ? null : newContact.name));
        }
    };


    @Override
    public void run(ApplicationArguments args) {
        log.info("WeChat 客户端开始启动");
        WeChatClient wechatClient = new WeChatClient();
        wechatClient.setListener(LISTENER);
        wechatClient.startup();
        log.info("WeChat 客户端启动完成");
    }
}
