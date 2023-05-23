package com.github.wmhxx.application.api.wechat.protocol;


public class ReqSync {
    public final BaseRequest BaseRequest;
    public final RspInit.SyncKey SyncKey;
    public final int rr;

    public ReqSync(BaseRequest baseRequest, RspInit.SyncKey syncKey) {
        this.BaseRequest = baseRequest;
        this.SyncKey = syncKey;
        this.rr = (int) (~(System.currentTimeMillis()));
    }
}
