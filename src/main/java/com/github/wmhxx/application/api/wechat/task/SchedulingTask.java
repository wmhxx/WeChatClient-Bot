package com.github.wmhxx.application.api.wechat.task;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.wmhxx.application.api.wechat.config.redis.RedisConstant;
import com.github.wmhxx.application.api.wechat.config.redis.RedisServiceImpl;
import com.github.wmhxx.application.api.wechat.entity.message.WxMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class SchedulingTask {

    @Resource
    private RedisServiceImpl redisService;


//    /**
//     * 更新WeiBo数据
//     */
//    @Scheduled(cron = "0 0/10 * * * ?")
//    public void updateWeiBoData() {
//        String body = HttpUtil.createGet("https://www.cnuseful.com/api/index/weiboHot")
//                .execute().body();
//        JSONObject jsonObject = JSONUtil.parseObj(body);
//        StringBuilder response = new StringBuilder("=== 最新微博热点 ===").append(WxMsg.LINE).append(WxMsg.LINE);
//        JSONArray data = jsonObject.getJSONArray("data");
//        if (ObjectUtil.isEmpty(data)) {
//            return;
//        }
//        AtomicInteger i = new AtomicInteger(1);
//        data.forEach(item -> {
//            String hotWord = new JSONObject(item).getStr("hot_word");
//            response.append("【").append(i.getAndIncrement()).append("】").append(hotWord).append(WxMsg.LINE);
//        });
//        String text = response.toString();
//        redisService.set(RedisConstant.WEI_BO_DATA, text);
//        log.info("更新微博数据成功");
//    }

}
