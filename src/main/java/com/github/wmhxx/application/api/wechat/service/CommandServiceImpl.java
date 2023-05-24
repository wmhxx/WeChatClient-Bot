package com.github.wmhxx.application.api.wechat.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.github.wmhxx.application.api.wechat.config.redis.RedisServiceImpl;
import com.github.wmhxx.application.api.wechat.entity.dto.HotResponseDTO;
import com.github.wmhxx.application.api.wechat.entity.dto.MusicResponseDTO;
import com.github.wmhxx.application.api.wechat.entity.message.WxMsg;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class CommandServiceImpl {


    public Map<String, Function<String, String>> textMap = new HashMap<String, Function<String, String>>() {{
        this.put("微博热点", CommandServiceImpl.this::weiboHot);
        this.put("微博热搜", CommandServiceImpl.this::weiboHot);
        this.put("虎扑热点", CommandServiceImpl.this::huPuHot);
        this.put("知乎热点", CommandServiceImpl.this::zhiHuHot);
        this.put("抖音热点", CommandServiceImpl.this::douYinHot);
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
        this.put("随机歌曲", CommandServiceImpl.this::randomMusic);
    }};

    private String weiboHot(String text) {
        String response = HttpUtil.get("https://api.vvhan.com/api/hotlist?type=wbHot");
        return HotResponseDTO.getResponse(response);
    }

    private String huPuHot(String text) {
        String response = HttpUtil.get("https://api.vvhan.com/api/hotlist?type=huPu");
        return HotResponseDTO.getResponse(response);
    }

    private String zhiHuHot(String text) {
        String response = HttpUtil.get("https://api.vvhan.com/api/hotlist?type=zhihuHot");
        return HotResponseDTO.getResponse(response);
    }

    private String douYinHot(String text) {
        String response = HttpUtil.get("https://api.vvhan.com/api/hotlist?type=douyinHot");
        return HotResponseDTO.getResponse(response);
    }


    private String lickDogDiary(String text) {
        return HttpUtil.get("https://cloud.qqshabi.cn/api/tiangou/api.php");
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

    private File randomMusic(String text){
        String response = HttpUtil.get("https://api.vvhan.com/api/rand.music?type=json&sort=%E7%83%AD%E6%AD%8C%E6%A6%9C");
        MusicResponseDTO musicResponseDTO = JSONUtil.toBean(response, MusicResponseDTO.class);
        String mp3url = musicResponseDTO.getInfo().getMp3url().replaceAll("\"","");
        final byte[] byteArray = HttpUtil.downloadBytes(mp3url);
        File file = FileUtil.createTempFile(".silk",true);
        return FileUtil.writeBytes(byteArray, file);
    }


    private String helpCommand(String s) {
        StringBuilder builder = new StringBuilder();
        builder.append("🎁 ►►► 常用命令 ◄◄◄ 🎁");
        builder.append(WxMsg.LINE);
        builder.append(WxMsg.LINE);
        builder.append("📕📕📕 无聊功能 📕📕📕");
        builder.append(WxMsg.LINE);
        builder.append("🚀 【微博热点】: 回复 ").append("微博热搜、微博热点");
        builder.append(WxMsg.LINE);
        builder.append("🚀 【虎扑热点】: 回复 ").append("虎扑热点");
        builder.append(WxMsg.LINE);
        builder.append("🚀 【知乎热点】: 回复 ").append("知乎热点");
        builder.append(WxMsg.LINE);
        builder.append("🚀 【抖音热点】: 回复 ").append("抖音热点");
        builder.append(WxMsg.LINE);
        builder.append("🚀 【舔狗日记】: 回复 ").append("舔狗日记、舔狗大全");
        builder.append(WxMsg.LINE);
        builder.append("🚀 【清凉一下】: 回复 ").append("清凉一下、美女图片");
        builder.append(WxMsg.LINE);
        builder.append("🚀 【摸鱼日历】: 回复 ").append("摸鱼日历");
        builder.append(WxMsg.LINE);
        builder.append("🚀 【随机歌曲】: 回复 ").append("随机歌曲");
        return builder.toString();
    }

}
