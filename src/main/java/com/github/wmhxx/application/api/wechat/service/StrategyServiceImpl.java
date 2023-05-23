package com.github.wmhxx.application.api.wechat.service;

import com.github.wmhxx.application.api.wechat.WeChatClient;
import com.github.wmhxx.application.api.wechat.config.redis.RedisConstant;
import com.github.wmhxx.application.api.wechat.config.redis.RedisServiceImpl;
import com.github.wmhxx.application.api.wechat.entity.message.WXMessage;
import com.github.wmhxx.application.api.wechat.entity.message.WXText;
import com.github.wmhxx.application.api.wechat.entity.message.WxMsg;
import com.github.wmhxx.application.api.wechat.utils.MsgUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 策略服务impl
 *
 * @author wmhxx
 * @date 2023/05/22
 */
@Slf4j
@Component
public class StrategyServiceImpl {

    @Resource
    private RedisServiceImpl redisService;

    @Resource
    private CommandServiceImpl commandService;

    public void getClient(WeChatClient client, WXMessage message) {
        //群组逻辑策略
        if (message instanceof WXText && message.fromUser != null && !message.fromUser.id
                .equals(client.userMe().id) && message.fromGroup != null) {
            this.strategyGroup(client, message);
        }
    }


    private void strategyGroup(WeChatClient client, WXMessage message) {
        log.info("收到文字消息。来自群: {}，用户: {}，内容: {}", message.fromGroup.name, message.fromUser.name, message.content);
        String prefix = this.getConfig().get(RedisConstant.Prefix);
        //是否包含指定的指令信息 不包含跳过即可
        String content = message.content;
        prefix = prefix + WxMsg.AT_ME_SPACE;
        if (!content.contains(prefix)) {
            return;
        }

        //是否发送的内容为白名单群组的内容
        Set<String> whiteListGorp = this.getWhiteListGorp();
        if (!whiteListGorp.contains(message.fromGroup.name)) {
            return;
        }
        content = content.replaceAll(prefix, "");

        for (String keyWord : commandService.textMap.keySet()) {
            //匹配某命令
            if (content.contains(keyWord)) {
                String response = commandService.textMap.get(keyWord).apply(content);
                if (response != null) {
                    client.sendText(message.fromGroup, MsgUtil.quote(message, response));
                }
                break;
            }
        }


        for (String keyWord : commandService.imgMap.keySet()) {
            //匹配某命令
            if (content.contains(keyWord)) {
                File file = commandService.imgMap.get(keyWord).apply(content);
                if (file != null) {
                    client.sendFile(message.fromGroup, file);
                }
                break;
            }
        }

    }


    private Map<String, String> getConfig() {
        Map<Object, Object> objectObjectMap = redisService.hGetAll(RedisConstant.SYSTEM_CONFIG);
        return objectObjectMap.entrySet().stream().collect(Collectors.toMap(k -> String.valueOf(k.getKey()), v -> String.valueOf(v.getValue())));
    }

    private Set<String> getWhiteListGorp() {
        return redisService.sGet(RedisConstant.WHITE_LIST_GORP).stream().map(String::valueOf).collect(Collectors.toSet());
    }
}
