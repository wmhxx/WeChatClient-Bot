package com.github.wmhxx.application.api.wechat.protocol;


import java.util.ArrayList;

public class RspGetContact {
    public BaseResponse BaseResponse;
    public int MemberCount;
    public ArrayList<RspInit.User> MemberList;
    public int Seq;
}
