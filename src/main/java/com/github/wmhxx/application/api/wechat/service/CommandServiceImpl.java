package com.github.wmhxx.application.api.wechat.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.github.wmhxx.application.api.wechat.config.redis.RedisConstant;
import com.github.wmhxx.application.api.wechat.config.redis.RedisServiceImpl;
import com.github.wmhxx.application.api.wechat.entity.message.WxMsg;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class CommandServiceImpl {

    @Resource
    private RedisServiceImpl redisService;

    public Map<String, Function<String, String>> textMap = new HashMap<String, Function<String, String>>() {{
        this.put("微博热点", CommandServiceImpl.this::weiboCommand);
        this.put("微博热搜", CommandServiceImpl.this::weiboCommand);
        this.put("帮助", CommandServiceImpl.this::helpCommand);
        this.put("功能", CommandServiceImpl.this::helpCommand);
        this.put("功能列表", CommandServiceImpl.this::helpCommand);
        this.put("help", CommandServiceImpl.this::helpCommand);
        this.put("舔狗日记", CommandServiceImpl.this::lickDogDiary);
        this.put("舔狗大全", CommandServiceImpl.this::lickDogDiary);
    }};


    public Map<String, Function<String, File>> imgMap = new HashMap<String, Function<String, File>>() {{
        this.put("清凉一下", CommandServiceImpl.this::getGirlCommand);
        this.put("美女图片", CommandServiceImpl.this::getGirlCommand);
        this.put("摸鱼日历", CommandServiceImpl.this::moYuCommand);
    }};

    private String weiboCommand(String text) {
        Object response = redisService.get(RedisConstant.WEI_BO_DATA);
        return String.valueOf(response);
    }

    private String lickDogDiary(String text) {
        if (redisService.hasKey(RedisConstant.LICK_DOG_DIARY)) {
            return redisService.lRightPop(RedisConstant.LICK_DOG_DIARY);
        }
        return null;
    }

    private File getGirlCommand(String text) {
        final byte[] byteArray = HttpUtil.downloadBytes("https://api.vvhan.com/api/girl");
        File file = FileUtil.createTempFile();
        return FileUtil.writeBytes(byteArray, file);
    }

    private File moYuCommand(String text) {
        final byte[] byteArray = HttpUtil.downloadBytes("https://api.vvhan.com/api/moyu");
        File file = FileUtil.createTempFile();
        return FileUtil.writeBytes(byteArray, file);
    }


    private String helpCommand(String s) {
        StringBuilder builder = new StringBuilder();
        builder.append("🎁 ►►► 常用命令 ◄◄◄ 🎁");
        builder.append(WxMsg.LINE);
        builder.append(WxMsg.LINE);
        builder.append("📕📕📕 无聊功能 📕📕📕");
        builder.append(WxMsg.LINE);
        builder.append("🚀 【微博热搜】: 回复 ").append("微博热搜、微博热点");
        builder.append(WxMsg.LINE);
        builder.append("🚀 【舔狗日记】: 回复 ").append("舔狗日记、舔狗大全");
        builder.append(WxMsg.LINE);
        builder.append("🚀 【清凉一下】: 回复 ").append("清凉一下、美女图片");
        builder.append(WxMsg.LINE);
        builder.append("🚀 【摸鱼日历】: 回复 ").append("摸鱼日历");
        return builder.toString();
    }

}
