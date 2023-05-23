package com.github.wmhxx.application.api.wechat.utils;

import com.github.wmhxx.application.api.wechat.WeChatClient;
import me.xuxiaoxiao.xtools.common.XTools;
import me.xuxiaoxiao.xtools.common.http.executor.impl.XHttpExecutorImpl;

/**
 * 微信HTTP请求执行器，增加了User-Agent请求头，支持配置文件配置
 * <ul>
 * <li>[2019-05-30 09:31]XXX：初始创建</li>
 * </ul>
 *
 * @author XXX
 */
public class WXHttpExecutor extends XHttpExecutorImpl {
    public static final String CFG_USERAGENT = WeChatClient.CFG_PREFIX + "userAgent";
    public static final String CFG_USERAGENT_DEFAULT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0";

    private final String userAgent = XTools.cfgDef(CFG_USERAGENT, CFG_USERAGENT_DEFAULT);

    @Override
    public Response execute(Request request) throws Exception {
        request.setHeader("User-Agent", userAgent, false);
        return super.execute(request);
    }
}
