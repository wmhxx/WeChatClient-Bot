package com.github.wmhxx.application.api.wechat.protocol;

public class ReqRevokeMsg {
    public BaseRequest BaseRequest;
    public String ClientMsgId;
    public String SvrMsgId;
    public String ToUserName;

    public ReqRevokeMsg(BaseRequest baseRequest, String clientMsgId, String serverMsgId, String toUserName) {
        this.BaseRequest = baseRequest;
        this.ClientMsgId = clientMsgId;
        this.SvrMsgId = serverMsgId;
        this.ToUserName = toUserName;
    }
}
