package com.github.wmhxx.application.api.wechat;

import com.github.wmhxx.application.api.wechat.entity.contact.WXContact;
import com.github.wmhxx.application.api.wechat.entity.contact.WXGroup;
import com.github.wmhxx.application.api.wechat.entity.contact.WXUser;
import com.github.wmhxx.application.api.wechat.entity.message.*;
import com.github.wmhxx.application.api.wechat.protocol.*;
import com.github.wmhxx.application.api.wechat.utils.WXHttpExecutor;
import lombok.extern.slf4j.Slf4j;
import me.xuxiaoxiao.xtools.common.XTools;
import me.xuxiaoxiao.xtools.common.http.XHttpTools;
import me.xuxiaoxiao.xtools.common.http.executor.impl.XRequest;
import me.xuxiaoxiao.xtools.common.time.XTimeTools;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpCookie;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模拟网页微信客户端
 */
@Slf4j
public final class WeChatClient {
    public static final String CFG_PREFIX = "cn.wmhxx$WeChatClient-Bot$";
    public static final String LOG_TAG = "WeChatClient-Bot";

    public static final String LOGIN_TIMEOUT = "登陆超时";
    public static final String LOGIN_EXCEPTION = "登陆异常";
    public static final String INIT_EXCEPTION = "初始化异常";
    public static final String LISTEN_EXCEPTION = "监听异常";

    public static final int STATUS_EXCEPTION = -1;
    public static final int STATUS_READY = 0;
    public static final int STATUS_SCAN = 1;
    public static final int STATUS_PERMIT = 2;
    public static final int STATUS_WORKING = 3;
    public static final int STATUS_LOGOUT = 4;

    private static final Pattern REX_GROUP_MSG = Pattern.compile("(@[0-9a-zA-Z]+):<br/>([\\s\\S]*)");
    private static final Pattern REX_REVOKE_ID = Pattern.compile("&lt;msgid&gt;(\\d+)&lt;/msgid&gt;");
    private static final Pattern REX_REVOKE_REPLACE = Pattern.compile("&lt;replacemsg&gt;&lt;!\\[CDATA\\[([\\s\\S]*)]]&gt;&lt;/replacemsg&gt;");

    private final WeChatThread wxThread = new WeChatThread();
    private final WeChatContacts wxContacts = new WeChatContacts();
    private final WeChatApi wxAPI = new WeChatApi();
    private volatile WeChatListener wxListener;
    private volatile int status = STATUS_READY;

    /**
     * 处理监听器，二维码事件
     *
     * @param qrcode 二维码地址
     */
    private void handleQRCode(@Nonnull String qrcode) {
        this.status = STATUS_SCAN;
        WeChatListener listener = this.wxListener;
        if (listener != null) {
            listener.onQRCode(this, qrcode);
        }
    }

    /**
     * 处理监听器，头像事件
     *
     * @param base64Avatar base64编码头像
     */
    private void handleAvatar(@Nonnull String base64Avatar) {
        this.status = STATUS_PERMIT;
        WeChatListener listener = this.wxListener;
        if (listener != null) {
            listener.onAvatar(this, base64Avatar);
        }
    }

    /**
     * 处理监听器，异常事件
     *
     * @param reason 异常信息
     */
    private void handleFailure(@Nonnull String reason) {
        this.status = STATUS_EXCEPTION;
        WeChatListener listener = this.wxListener;
        if (listener != null) {
            listener.onFailure(this, reason);
        }
    }

    /**
     * 处理监听器，登录完成事件
     */
    private void handleLogin() {
        this.status = STATUS_WORKING;
        WeChatListener listener = this.wxListener;
        if (listener != null) {
            listener.onLogin(this);
        }
    }

    /**
     * 处理监听器，新消息事件
     *
     * @param message 微信消息
     */
    private void handleMessage(WXMessage message) {
        this.status = STATUS_WORKING;
        WeChatListener listener = this.wxListener;
        if (listener != null) {
            listener.onMessage(this, message);
        }
    }

    /**
     * 处理监听器，联系人变动事件
     *
     * @param oldContact 旧联系人，新增联系人时为null
     * @param newContact 新联系人，删除联系人时为null
     */
    private void handleContact(WXContact oldContact, WXContact newContact) {
        this.status = STATUS_WORKING;
        WeChatListener listener = this.wxListener;
        if (listener != null) {
            listener.onContact(this, oldContact, newContact);
        }
    }

    /**
     * 处理监听器，退出登录事件
     */
    private void handleLogout() {
        this.status = STATUS_LOGOUT;
        WeChatListener listener = this.wxListener;
        if (listener != null) {
            listener.onLogout(this);
        }
    }

    /**
     * 获取并保存不限数量和类型的联系人信息
     *
     * @param userNames 逗号分隔的联系人userName
     */
    private void loadContacts(@Nonnull String userNames, boolean useCache) {
        if (!XTools.strEmpty(userNames)) {
            LinkedList<ReqBatchGetContact.Contact> contacts = new LinkedList<>();
            for (String userName : userNames.split(",")) {
                if (!XTools.strEmpty(userName)) {
                    contacts.add(new ReqBatchGetContact.Contact(userName, ""));
                }
            }
            loadContacts(contacts, useCache);
        }
    }

    /**
     * 获取并保存不限数量和类型的联系人信息
     *
     * @param contacts 要获取的联系人的列表，数量和类型不限
     */
    private void loadContacts(@Nonnull List<ReqBatchGetContact.Contact> contacts, boolean useCache) {
        if (useCache) {
            //不是群聊，并且已经获取过，就不再次获取
            contacts.removeIf(contact -> !contact.UserName.startsWith("@@") && wxContacts.getContact(contact.UserName) != null);
        }
        //拆分成每次50个联系人分批获取
        if (contacts.size() > 50) {
            LinkedList<ReqBatchGetContact.Contact> temp = new LinkedList<>();
            for (ReqBatchGetContact.Contact contact : contacts) {
                temp.add(contact);
                if (temp.size() >= 50) {
                    RspBatchGetContact rspBatchGetContact = wxAPI.webWeChatBatchGetContact(contacts);
                    for (RspInit.User user : rspBatchGetContact.ContactList) {
                        wxContacts.putContact(wxAPI.host, user);
                    }
                    temp.clear();
                }
            }
            contacts = temp;
        }
        if (contacts.size() > 0) {
            RspBatchGetContact rspBatchGetContact = wxAPI.webWeChatBatchGetContact(contacts);
            for (RspInit.User user : rspBatchGetContact.ContactList) {
                wxContacts.putContact(wxAPI.host, user);
            }
        }
    }

    /**
     * 打印Cookie和登录信息
     */
    public void dump() {
        try {
            XTools.logE(LOG_TAG, "微信用户：" + userMe().name);

            StringBuilder sbCookie = new StringBuilder("Cookie信息：");
            Field executor = WeChatApi.class.getDeclaredField("httpExecutor");
            executor.setAccessible(true);
            Field created = HttpCookie.class.getDeclaredField("whenCreated");
            created.setAccessible(true);
            for (HttpCookie cookie : ((WXHttpExecutor) executor.get(wxAPI)).getCookies()) {
                sbCookie.append("\n\t过期时间：").append(XTools.dateFormat(XTimeTools.FORMAT_YMDHMS, new Date((long) created.get(cookie) + cookie.getMaxAge() * 1000)));
                sbCookie.append("，键：").append(cookie.getName());
                sbCookie.append("，值：").append(cookie.getValue());
            }
            XTools.logE(LOG_TAG, sbCookie.toString());

            StringBuilder sbLogin = new StringBuilder("登录信息：");
            sbLogin.append("\n\thost：").append(wxAPI.host);
            sbLogin.append("\n\tuin：").append(wxAPI.uin);
            sbLogin.append("\n\tsid：").append(wxAPI.sid);
            sbLogin.append("\n\tdataTicket：").append(wxAPI.dataTicket);
            Field uuid = WeChatApi.class.getDeclaredField("uuid");
            uuid.setAccessible(true);
            sbLogin.append("\n\tuuid：").append(uuid.get(wxAPI));
            Field skey = WeChatApi.class.getDeclaredField("skey");
            skey.setAccessible(true);
            sbLogin.append("\n\tskey：").append(skey.get(wxAPI));
            Field passticket = WeChatApi.class.getDeclaredField("passticket");
            passticket.setAccessible(true);
            sbLogin.append("\n\tpassticket：").append(passticket.get(wxAPI));
            Field synckey = WeChatApi.class.getDeclaredField("synckey");
            synckey.setAccessible(true);
            sbLogin.append("\n\tsynckey：").append(synckey.get(wxAPI));
            Field syncCheckKey = WeChatApi.class.getDeclaredField("syncCheckKey");
            syncCheckKey.setAccessible(true);
            sbLogin.append("\n\tsyncCheckKey：").append(syncCheckKey.get(wxAPI));
            XTools.logE(LOG_TAG, sbLogin.toString().replace("%", "%%"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取客户端的监听器
     *
     * @return 监听器对象
     */
    @Nullable
    public WeChatListener getListener() {
        return this.wxListener;
    }

    /**
     * 设置客户端的监听器
     *
     * @param listener 监听器对象
     */
    public void setListener(@Nonnull WeChatListener listener) {
        this.wxListener = listener;
    }

    /**
     * 启动客户端，注意：一个客户端类的实例只能被启动一次
     */
    public void startup() {
        wxThread.start();
    }

    /**
     * 获取客户端的状态
     *
     * @return 客户端的当前状态
     */
    public int status() {
        return this.status;
    }

    /**
     * 关闭客户端，注意：关闭后的客户端不能再被启动
     */
    public void shutdown() {
        wxAPI.webWeChatLogout();
        wxThread.interrupt();
    }

    /**
     * 获取当前登录的用户信息
     *
     * @return 当前登录的用户信息
     */
    public WXUser userMe() {
        return wxContacts.getMe();
    }

    /**
     * 根据userId获取用户好友
     *
     * @param userId 好友的id
     * @return 好友的信息
     */
    @Nullable
    public WXUser userFriend(@Nonnull String userId) {
        return wxContacts.getFriend(userId);
    }

    /**
     * 获取用户所有好友
     *
     * @return 用户所有好友
     */
    @Nonnull
    public HashMap<String, WXUser> userFriends() {
        return wxContacts.getFriends();
    }

    /**
     * 根据群id获取群信息
     *
     * @param groupId 群id
     * @return 群信息
     */
    @Nullable
    public WXGroup userGroup(@Nonnull String groupId) {
        return wxContacts.getGroup(groupId);
    }

    /**
     * 获取用户所有群
     *
     * @return 用户所有群
     */
    @Nonnull
    public HashMap<String, WXGroup> userGroups() {
        return wxContacts.getGroups();
    }

    /**
     * 根据联系人id获取用户联系人信息
     *
     * @param contactId 联系人id
     * @return 联系人信息
     */
    @Nullable
    public WXContact userContact(@Nonnull String contactId) {
        return wxContacts.getContact(contactId);
    }

    /**
     * 发送文字消息
     *
     * @param wxContact 目标联系人
     * @param text      要发送的文字
     * @return 文本消息
     */
    @Nonnull
    public WXText sendText(@Nonnull WXContact wxContact, @Nonnull String text) {
        log.info("向（{}）发送消息：{}", wxContact.id, text);
        RspSendMsg rspSendMsg = wxAPI.webWeChatSendMsg(new ReqSendMsg.Msg(RspSync.AddMsg.TYPE_TEXT, null, 0, text, null, wxContacts.getMe().id, wxContact.id));
        WXText wxText = new WXText();
        wxText.id = Long.parseLong(rspSendMsg.MsgID);
        wxText.idLocal = Long.parseLong(rspSendMsg.LocalID);
        wxText.timestamp = System.currentTimeMillis();
        wxText.fromGroup = null;
        wxText.fromUser = wxContacts.getMe();
        wxText.toContact = wxContact;
        wxText.content = text;
        return wxText;
    }

    /**
     * 发送文件消息，可以是图片，动图，视频，文本等文件
     *
     * @param wxContact 目标联系人
     * @param file      要发送的文件
     * @return 图像或附件消息
     */
    @Nullable
    public WXMessage sendFile(@Nonnull WXContact wxContact, @Nonnull File file) {
        String suffix = WeChatTools.fileSuffix(file);
        if ("mp4".equals(suffix) && file.length() >= 20L * 1024L * 1024L) {
            XTools.logW(LOG_TAG, String.format("向（%s）发送的视频文件大于20M，无法发送", wxContact.id));
            return null;
        } else {
            try {
                XTools.logN(LOG_TAG, String.format("向（%s）发送文件：%s", wxContact.id, file.getAbsolutePath()));
                String mediaId = null, aesKey = null, signature = null;
                if (file.length() >= 25L * 1024L * 1024L) {
                    RspCheckUpload rspCheckUpload = wxAPI.webWeChatCheckUpload(file, wxContacts.getMe().id, wxContact.id);
                    mediaId = rspCheckUpload.MediaId;
                    aesKey = rspCheckUpload.AESKey;
                    signature = rspCheckUpload.Signature;
                }
                if (XTools.strEmpty(mediaId)) {
                    RspUploadMedia rspUploadMedia = wxAPI.webWeChatUploadMedia(wxContacts.getMe().id, wxContact.id, file, aesKey, signature);
                    mediaId = rspUploadMedia.MediaId;
                }

                if (!XTools.strEmpty(mediaId)) {
                    switch (WeChatTools.fileType(file)) {
                        case "pic": {
                            RspSendMsg rspSendMsg = wxAPI.webwxsendmsgimg(new ReqSendMsg.Msg(RspSync.AddMsg.TYPE_IMAGE, mediaId, null, "", signature, wxContacts.getMe().id, wxContact.id));
                            WXImage wxImage = new WXImage();
                            wxImage.id = Long.valueOf(rspSendMsg.MsgID);
                            wxImage.idLocal = Long.valueOf(rspSendMsg.LocalID);
                            wxImage.timestamp = System.currentTimeMillis();
                            wxImage.fromGroup = null;
                            wxImage.fromUser = wxContacts.getMe();
                            wxImage.toContact = wxContact;
                            wxImage.imgWidth = 0;
                            wxImage.imgHeight = 0;
                            wxImage.image = wxAPI.webWeChatGetMsgImg(wxImage.id, "slave");
                            wxImage.origin = file;
                            return wxImage;
                        }
                        case "video": {
                            RspSendMsg rspSendMsg = wxAPI.webwxsendvideomsg(new ReqSendMsg.Msg(RspSync.AddMsg.TYPE_VIDEO, mediaId, null, "", signature, wxContacts.getMe().id, wxContact.id));
                            WXVideo wxVideo = new WXVideo();
                            wxVideo.id = Long.valueOf(rspSendMsg.MsgID);
                            wxVideo.idLocal = Long.valueOf(rspSendMsg.LocalID);
                            wxVideo.timestamp = System.currentTimeMillis();
                            wxVideo.fromGroup = null;
                            wxVideo.fromUser = wxContacts.getMe();
                            wxVideo.toContact = wxContact;
                            wxVideo.imgWidth = 0;
                            wxVideo.imgHeight = 0;
                            wxVideo.image = wxAPI.webWeChatGetMsgImg(wxVideo.id, "slave");
                            wxVideo.videoLength = 0;
                            wxVideo.video = file;
                            return wxVideo;
                        }
                        default:
                            if ("gif".equals(suffix)) {
                                RspSendMsg rspSendMsg = wxAPI.webwxsendemoticon(new ReqSendMsg.Msg(RspSync.AddMsg.TYPE_EMOJI, mediaId, 2, "", signature, wxContacts.getMe().id, wxContact.id));
                                WXImage wxImage = new WXImage();
                                wxImage.id = Long.valueOf(rspSendMsg.MsgID);
                                wxImage.idLocal = Long.valueOf(rspSendMsg.LocalID);
                                wxImage.timestamp = System.currentTimeMillis();
                                wxImage.fromGroup = null;
                                wxImage.fromUser = wxContacts.getMe();
                                wxImage.toContact = wxContact;
                                wxImage.imgWidth = 0;
                                wxImage.imgHeight = 0;
                                wxImage.image = file;
                                wxImage.origin = file;
                                return wxImage;
                            } else {
                                StringBuilder sbAppMsg = new StringBuilder();
                                sbAppMsg.append("<appmsg appid='wxeb7ec651dd0aefa9' sdkver=''>");
                                sbAppMsg.append("<title>").append(file.getName()).append("</title>");
                                sbAppMsg.append("<des></des>");
                                sbAppMsg.append("<action></action>");
                                sbAppMsg.append("<type>6</type>");
                                sbAppMsg.append("<content></content>");
                                sbAppMsg.append("<url></url>");
                                sbAppMsg.append("<lowurl></lowurl>");
                                sbAppMsg.append("<appattach>");
                                sbAppMsg.append("<totallen>").append(file.length()).append("</totallen>");
                                sbAppMsg.append("<attachid>").append(mediaId).append("</attachid>");
                                sbAppMsg.append("<fileext>").append(XTools.strEmpty(suffix) ? "undefined" : suffix).append("</fileext>");
                                sbAppMsg.append("</appattach>");
                                sbAppMsg.append("<extinfo></extinfo>");
                                sbAppMsg.append("</appmsg>");
                                RspSendMsg rspSendMsg = wxAPI.webwxsendappmsg(new ReqSendMsg.Msg(6, null, null, sbAppMsg.toString(), signature, wxContacts.getMe().id, wxContact.id));
                                WXFile wxFile = new WXFile();
                                wxFile.id = Long.valueOf(rspSendMsg.MsgID);
                                wxFile.idLocal = Long.valueOf(rspSendMsg.LocalID);
                                wxFile.timestamp = System.currentTimeMillis();
                                wxFile.fromGroup = null;
                                wxFile.fromUser = wxContacts.getMe();
                                wxFile.toContact = wxContact;
                                wxFile.content = sbAppMsg.toString();
                                wxFile.fileSize = file.length();
                                wxFile.fileName = file.getName();
                                wxFile.fileId = mediaId;
                                wxFile.file = file;
                                return wxFile;
                            }
                    }
                } else {
                    XTools.logE(LOG_TAG, String.format("向（%s）发送的文件发送失败", wxContact.id));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 发送位置消息
     * <p>
     * 经纬度坐标可以通过腾讯坐标拾取工具获得(https://lbs.qq.com/tool/getpoint)
     * 其拾取的坐标默认格式为 lat,lon
     * </p>
     *
     * @param wxContact 目标联系人
     * @param lon       经度
     * @param lat       纬度
     * @param title     定位消息模块标题
     * @param lable     定位消息模块二级描述
     * @return 定位消息
     */
    @Nonnull
    public WXLocation sendLocation(@Nonnull WXContact wxContact, @Nonnull String lon, @Nonnull String lat, @Nonnull String title, @Nonnull String lable) {
        XTools.logN(LOG_TAG, String.format("向（%s）发送位置信息，坐标：%s,%s，说明：%s(%s)", wxContact.id, lon, lat, title, lable));
        StringBuilder sbLocationMsg = new StringBuilder();
        sbLocationMsg.append("<?xml version=\"1.0\"?>\n");
        sbLocationMsg.append("<msg>\n");
        sbLocationMsg.append("<location x=\"" + lat + "\" y=\"" + lon + "\" scale=\"15\" label=\"" + lable + "\" maptype=\"roadmap\" poiname=\"" + title + "\" poiid=\"City\" />\n");
        sbLocationMsg.append("</msg>\n");
        RspSendMsg rspSendMsg = wxAPI.webWeChatSendMsg(new ReqSendMsg.Msg(RspSync.AddMsg.TYPE_LOCATION, null, 0, sbLocationMsg.toString(), null, wxContacts.getMe().id, wxContact.id));
        WXLocation wxLocation = new WXLocation();
        wxLocation.id = Long.valueOf(rspSendMsg.MsgID);
        wxLocation.idLocal = Long.valueOf(rspSendMsg.LocalID);
        wxLocation.timestamp = System.currentTimeMillis();
        wxLocation.fromGroup = null;
        wxLocation.fromUser = wxContacts.getMe();
        wxLocation.toContact = wxContact;
        wxLocation.content = sbLocationMsg.toString();
        return wxLocation;
    }

    /**
     * 获取用户联系人，如果获取的联系人是群组，则会自动获取群成员的详细信息
     * <strong>在联系人列表中获取到的群，没有群成员，可以通过这个方法，获取群的详细信息</strong>
     *
     * @param contactId 联系人id
     * @return 联系人的详细信息
     */
    @Nullable
    public WXContact fetchContact(@Nonnull String contactId) {
        loadContacts(contactId, false);
        WXContact contact = wxContacts.getContact(contactId);
        if (contact instanceof WXGroup) {
            List<ReqBatchGetContact.Contact> contacts = new LinkedList<>();
            for (WXGroup.Member member : ((WXGroup) contact).members.values()) {
                contacts.add(new ReqBatchGetContact.Contact(member.id, contact.id));
            }
            loadContacts(contacts, true);
            ((WXGroup) contact).isDetail = true;
        }
        return contact;
    }

    /**
     * 获取用户头像
     *
     * @param wxContact 要获取头像文件的用户
     * @return 获取头像文件后的用户
     */
    @Nonnull
    public WXContact fetchAvatar(@Nonnull WXContact wxContact) {
        wxContact.avatarFile = XTools.http(XHttpTools.EXECUTOR, XRequest.GET(wxContact.avatarUrl)).file(wxAPI.folder.getAbsolutePath() + File.separator + String.format("avatar-%d.jpg", System.currentTimeMillis() + new Random().nextInt(1000)));
        return wxContact;
    }

    /**
     * 获取图片消息的大图
     *
     * @param wxImage 要获取大图的图片消息
     * @return 获取大图后的图片消息
     */
    @Nonnull
    public WXImage fetchImage(@Nonnull WXImage wxImage) {
        wxImage.origin = wxAPI.webWeChatGetMsgImg(wxImage.id, "big");
        return wxImage;
    }

    /**
     * 获取语音消息的语音文件
     *
     * @param wxVoice 语音消息
     * @return 获取语音文件后的语音消息
     */
    @Nonnull
    public WXVoice fetchVoice(@Nonnull WXVoice wxVoice) {
        wxVoice.voice = wxAPI.webWeChatGetVoice(wxVoice.id);
        return wxVoice;
    }

    /**
     * 获取视频消息的视频文件
     *
     * @param wxVideo 视频消息
     * @return 获取视频文件后的视频消息
     */
    @Nonnull
    public WXVideo fetchVideo(@Nonnull WXVideo wxVideo) {
        wxVideo.video = wxAPI.webWeChatGetVideo(wxVideo.id);
        return wxVideo;
    }

    /**
     * 获取文件消息的附件文件
     *
     * @param wxFile 文件消息
     * @return 获取附件文件后的文件消息
     */
    @Nonnull
    public WXFile fetchFile(@Nonnull WXFile wxFile) {
        wxFile.file = wxAPI.webWeChatGetMedia(wxFile.id, wxFile.fileName, wxFile.fileId, wxFile.fromUser.id);
        return wxFile;
    }

    /**
     * 撤回消息
     *
     * @param wxMessage 需要撤回的微信消息
     */
    public void revokeMsg(@Nonnull WXMessage wxMessage) {
        XTools.logN(LOG_TAG, String.format("撤回向（%s）发送的消息：%s，%s", wxMessage.toContact.id, wxMessage.idLocal, wxMessage.id));
        wxAPI.webWeChatRevokeMsg(wxMessage.idLocal, wxMessage.id, wxMessage.toContact.id);
    }

    /**
     * 同意好友申请
     *
     * @param wxVerify 好友验证消息
     */
    public void passVerify(@Nonnull WXVerify wxVerify) {
        XTools.logN(LOG_TAG, String.format("通过好友（%s）申请", wxVerify.userId));
        wxAPI.webWeChatVerifyUser(3, wxVerify.userId, wxVerify.ticket, "");
    }

    /**
     * 修改用户备注名
     *
     * @param wxUser 目标用户
     * @param remark 备注名称
     */
    public void editRemark(@Nonnull WXUser wxUser, @Nonnull String remark) {
        XTools.logN(LOG_TAG, String.format("修改（%s）的备注：%s", wxUser.id, remark));
        wxAPI.webWeChatOplog(ReqOplog.CMD_REMARK, ReqOplog.OP_NONE, wxUser.id, remark);
    }

    /**
     * 设置联系人置顶状态
     *
     * @param wxContact 需要设置置顶状态的联系人
     * @param isTop     是否置顶
     */
    public void topContact(@Nonnull WXContact wxContact, boolean isTop) {
        XTools.logN(LOG_TAG, String.format("设置（%s）的置顶状态：%s", wxContact.id, isTop));
        wxAPI.webWeChatOplog(ReqOplog.CMD_TOP, isTop ? ReqOplog.OP_TOP_TRUE : ReqOplog.OP_TOP_FALSE, wxContact.id, null);
    }

    /**
     * 修改聊天室名称
     *
     * @param wxGroup 目标聊天室
     * @param name    目标名称
     */
    public void setGroupName(@Nonnull WXGroup wxGroup, @Nonnull String name) {
        XTools.logN(LOG_TAG, String.format("为群（%s）修改名称：%s", wxGroup.id, name));
        wxAPI.webWeChatUpdateChartRoom(wxGroup.id, "modtopic", name, new LinkedList<String>());
    }

    /**
     * 模拟网页微信客户端工作线程
     */
    private class WeChatThread extends Thread {

        @Override
        public void run() {
            int loginCount = 0;
            while (!isInterrupted()) {
                //用户登录
                XTools.logD(LOG_TAG, "正在登录");
                String loginErr = login();
                if (!XTools.strEmpty(loginErr)) {
                    XTools.logE(LOG_TAG, String.format("登录出现错误：%s", loginErr));
                    handleFailure(loginErr);
                    return;
                }
                //用户初始化
                XTools.logD(LOG_TAG, "正在初始化");
                String initErr = initial();
                if (!XTools.strEmpty(initErr)) {
                    XTools.logE(LOG_TAG, String.format("初始化出现错误：%s", initErr));
                    handleFailure(initErr);
                    return;
                }
                handleLogin();
                //同步消息
                log.info("正在监听消息");
                String listenErr = listen();
                if (!XTools.strEmpty(listenErr)) {
                    if (loginCount++ > 10) {
                        handleFailure(listenErr);
                        return;
                    } else {
                        continue;
                    }
                }
                //退出登录
                XTools.logD(LOG_TAG, "正在退出登录");
                handleLogout();
                return;
            }
        }

        /**
         * 用户登录
         *
         * @return 登录时异常原因，为null表示正常登录
         */
        @Nullable
        private String login() {
            try {
                if (XTools.strEmpty(wxAPI.sid)) {
                    String qrCode = wxAPI.jsLogin();
                    XTools.logD(LOG_TAG, String.format("等待扫描二维码：%s", qrCode));
                    handleQRCode(qrCode);
                    while (true) {
                        RspLogin rspLogin = wxAPI.login();
                        switch (rspLogin.code) {
                            case 200:
                                XTools.logD(LOG_TAG, "已授权登录");
                                wxAPI.webWeChatNewLoginPage(rspLogin.redirectUri);
                                return null;
                            case 201:
                                XTools.logD(LOG_TAG, "已扫描二维码");
                                handleAvatar(rspLogin.userAvatar);
                                break;
                            case 408:
                                XTools.logD(LOG_TAG, "等待授权登录");
                                break;
                            default:
                                XTools.logW(LOG_TAG, "登录超时");
                                return LOGIN_TIMEOUT;
                        }
                    }
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                XTools.logW(LOG_TAG, e, "登录异常");
                return LOGIN_EXCEPTION;
            }
        }

        /**
         * 初始化
         *
         * @return 初始化异常原因，为null表示正常初始化
         */
        @Nullable
        private String initial() {
            try {
                //通过Cookie获取重要参数
                XTools.logD(LOG_TAG, "正在获取Cookie");
                List<HttpCookie> cookies = XHttpTools.EXECUTOR.getCookies();
                for (HttpCookie cookie : cookies) {
                    if ("wxsid".equalsIgnoreCase(cookie.getName())) {
                        wxAPI.sid = cookie.getValue();
                    } else if ("wxuin".equalsIgnoreCase(cookie.getName())) {
                        wxAPI.uin = cookie.getValue();
                    } else if ("webwx_data_ticket".equalsIgnoreCase(cookie.getName())) {
                        wxAPI.dataTicket = cookie.getValue();
                    }
                }

                //获取自身信息
                XTools.logD(LOG_TAG, "正在获取自身信息");
                RspInit rspInit = wxAPI.webWeChatInit();
                wxContacts.setMe(wxAPI.host, rspInit.User);

                //获取并保存最近联系人
                XTools.logD(LOG_TAG, "正在获取并保存最近联系人");
                loadContacts(rspInit.ChatSet, true);

                //发送初始化状态信息
                wxAPI.webWeChatStatusNotify(wxContacts.getMe().id, WXNotify.NOTIFY_INITED);

                //获取好友、保存的群聊、公众号列表。
                //这里获取的群没有群成员，不过也不能用fetchContact方法暴力获取群成员，因为这样数据量会很大
                XTools.logD(LOG_TAG, "正在获取好友、群、公众号列表");
                RspGetContact rspGetContact = wxAPI.webWechatGetContact();
                for (RspInit.User user : rspGetContact.MemberList) {
                    wxContacts.putContact(wxAPI.host, user);
                }

                return null;
            } catch (Exception e) {
                e.printStackTrace();
                XTools.logW(LOG_TAG, String.format("初始化异常：%s", e.getMessage()));
                return INIT_EXCEPTION;
            }
        }

        /**
         * 循环同步消息
         *
         * @return 同步消息的异常原因，为null表示正常结束
         */
        @Nullable
        private String listen() {
            int retryCount = 0;
            try {
                while (!isInterrupted()) {
                    RspSyncCheck rspSyncCheck;
                    try {
                        log.info("正在监听信息");
                        rspSyncCheck = wxAPI.synccheck();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (retryCount++ < 5) {
                            XTools.logW(LOG_TAG, e, String.format("监听异常，重试第%d次", retryCount));
                            continue;
                        } else {
                            XTools.logE(LOG_TAG, e, "监听异常，重试次数过多");
                            return LISTEN_EXCEPTION;
                        }
                    }
                    retryCount = 0;
                    if (rspSyncCheck.retcode > 0) {
                        XTools.logW(LOG_TAG, String.format("停止监听信息：%d", rspSyncCheck.retcode));
                        return null;
                    } else if (rspSyncCheck.selector > 0) {
                        RspSync rspSync = wxAPI.webwxsync();
                        if (rspSync.DelContactList != null) {
                            //删除好友立刻触发
                            //删除群后的任意一条消息触发
                            //被移出群不会触发（会收到一条被移出群的addMsg）
                            for (RspInit.User user : rspSync.DelContactList) {
                                WXContact oldContact = wxContacts.rmvContact(user.UserName);
                                if (oldContact != null && !XTools.strEmpty(oldContact.name)) {
                                    XTools.logN(LOG_TAG, String.format("删除联系人（%s）", user.UserName));
                                    handleContact(oldContact, null);
                                }
                            }
                        }
                        if (rspSync.ModContactList != null) {
                            //添加好友立刻触发
                            //被拉入已存在的群立刻触发
                            //被拉入新群第一条消息触发（同时收到2条addMsg，一条被拉入群，一条聊天消息）
                            //群里有人加入或群里踢人或修改群信息之后第一条信息触发
                            for (RspInit.User user : rspSync.ModContactList) {
                                //由于在这里获取到的联系人（无论是群还是用户）的信息是不全的，所以使用接口重新获取
                                WXContact oldContact = wxContacts.getContact(user.UserName);
                                if (oldContact != null && XTools.strEmpty(oldContact.name)) {
                                    wxContacts.rmvContact(user.UserName);
                                    oldContact = null;
                                }
                                WXContact newContact = fetchContact(user.UserName);
                                if (newContact != null && XTools.strEmpty(newContact.name)) {
                                    wxContacts.rmvContact(user.UserName);
                                    newContact = null;
                                }
                                if (oldContact != null || newContact != null) {
                                    XTools.logN(LOG_TAG, String.format("变更联系人（%s）", user.UserName));
                                    handleContact(oldContact, newContact);
                                }
                            }
                        }
                        if (rspSync.AddMsgList != null) {
                            for (RspSync.AddMsg addMsg : rspSync.AddMsgList) {
                                //接收到的消息，文字、图片、语音、地理位置等等
                                WXMessage wxMessage = parseMessage(addMsg);
                                if (wxMessage instanceof WXNotify) {
                                    //状态更新消息，需要处理后再交给监听器
                                    WXNotify wxNotify = (WXNotify) wxMessage;
                                    if (wxNotify.notifyCode == WXNotify.NOTIFY_SYNC_CONV) {
                                        //会话同步，网页微信仅仅只获取了相关联系人详情
                                        loadContacts(wxNotify.notifyContact, false);
                                    }
                                }
                                //最后交给监听器处理
                                handleMessage(wxMessage);
                            }
                        }
                    }
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                XTools.logW(LOG_TAG, e, "监听消息异常");
                return LISTEN_EXCEPTION;
            }
        }

        @Nonnull
        private <T extends WXMessage> T parseCommon(@Nonnull RspSync.AddMsg msg, @Nonnull T message) {
            message.id = msg.MsgId;
            message.idLocal = msg.MsgId;
            message.timestamp = msg.CreateTime * 1000;
            if (msg.FromUserName.startsWith("@@")) {
                //是群消息
                message.fromGroup = (WXGroup) wxContacts.getContact(msg.FromUserName);
                if (message.fromGroup == null || !message.fromGroup.isDetail || message.fromGroup.members.isEmpty()) {
                    //如果群不存在，或者是未获取成员的群。获取并保存群的详细信息
                    message.fromGroup = (WXGroup) fetchContact(msg.FromUserName);
                }
                Matcher mGroupMsg = REX_GROUP_MSG.matcher(msg.Content);
                if (mGroupMsg.matches()) {
                    //是群成员发送的消息
                    message.fromUser = (WXUser) wxContacts.getContact(mGroupMsg.group(1));
                    if (message.fromUser == null) {
                        //未获取成员。首先获取并保存群的详细信息，然后获取群成员信息
                        fetchContact(msg.FromUserName);
                        message.fromUser = (WXUser) wxContacts.getContact(mGroupMsg.group(1));
                    }
                    message.toContact = wxContacts.getContact(msg.ToUserName);
                    if (message.toContact == null) {
                        message.toContact = fetchContact(msg.ToUserName);
                    }
                    message.content = mGroupMsg.group(2);
                } else {
                    //不是群成员发送的消息
                    message.fromUser = null;
                    message.toContact = wxContacts.getContact(msg.ToUserName);
                    if (message.toContact == null) {
                        message.toContact = fetchContact(msg.ToUserName);
                    }
                    message.content = msg.Content;
                }
            } else {
                //不是群消息
                message.fromGroup = null;
                message.fromUser = (WXUser) wxContacts.getContact(msg.FromUserName);
                if (message.fromUser == null) {
                    //联系人不存在（一般不会出现这种情况），手动获取联系人
                    message.fromUser = (WXUser) fetchContact(msg.FromUserName);
                }
                message.toContact = wxContacts.getContact(msg.ToUserName);
                if (message.toContact == null) {
                    message.toContact = fetchContact(msg.ToUserName);
                }
                message.content = msg.Content;
            }
            return message;
        }

        @Nonnull
        private WXMessage parseMessage(@Nonnull RspSync.AddMsg msg) {
            try {
                switch (msg.MsgType) {
                    case RspSync.AddMsg.TYPE_TEXT: {
                        if (msg.SubMsgType == 0) {
                            return parseCommon(msg, new WXText());
                        } else if (msg.SubMsgType == 48) {
                            WXLocation wxLocation = parseCommon(msg, new WXLocation());
                            wxLocation.locationName = wxLocation.content.substring(0, wxLocation.content.indexOf(':'));
                            wxLocation.locationImage = String.format("https://%s%s", wxAPI.host, wxLocation.content.substring(wxLocation.content.indexOf(':') + ":<br/>".length()));
                            wxLocation.locationUrl = msg.Url;
                            return wxLocation;
                        }
                        break;
                    }
                    case RspSync.AddMsg.TYPE_IMAGE: {
                        WXImage wxImage = parseCommon(msg, new WXImage());
                        wxImage.imgWidth = msg.ImgWidth;
                        wxImage.imgHeight = msg.ImgHeight;
                        wxImage.image = wxAPI.webWeChatGetMsgImg(msg.MsgId, "slave");
                        return wxImage;
                    }
                    case RspSync.AddMsg.TYPE_VOICE: {
                        WXVoice wxVoice = parseCommon(msg, new WXVoice());
                        wxVoice.voiceLength = msg.VoiceLength;
                        return wxVoice;
                    }
                    case RspSync.AddMsg.TYPE_VERIFY: {
                        WXVerify wxVerify = parseCommon(msg, new WXVerify());
                        wxVerify.userId = msg.RecommendInfo.UserName;
                        wxVerify.userName = msg.RecommendInfo.NickName;
                        wxVerify.signature = msg.RecommendInfo.Signature;
                        wxVerify.province = msg.RecommendInfo.Province;
                        wxVerify.city = msg.RecommendInfo.City;
                        wxVerify.gender = msg.RecommendInfo.Sex;
                        wxVerify.verifyFlag = msg.RecommendInfo.VerifyFlag;
                        wxVerify.ticket = msg.RecommendInfo.Ticket;
                        return wxVerify;
                    }
                    case RspSync.AddMsg.TYPE_RECOMMEND: {
                        WXRecommend wxRecommend = parseCommon(msg, new WXRecommend());
                        wxRecommend.userId = msg.RecommendInfo.UserName;
                        wxRecommend.userName = msg.RecommendInfo.NickName;
                        wxRecommend.gender = msg.RecommendInfo.Sex;
                        wxRecommend.signature = msg.RecommendInfo.Signature;
                        wxRecommend.province = msg.RecommendInfo.Province;
                        wxRecommend.city = msg.RecommendInfo.City;
                        wxRecommend.verifyFlag = msg.RecommendInfo.VerifyFlag;
                        return wxRecommend;
                    }
                    case RspSync.AddMsg.TYPE_VIDEO: {
                        //视频貌似可以分片，后期测试
                        WXVideo wxVideo = parseCommon(msg, new WXVideo());
                        wxVideo.imgWidth = msg.ImgWidth;
                        wxVideo.imgHeight = msg.ImgHeight;
                        wxVideo.videoLength = msg.PlayLength;
                        wxVideo.image = wxAPI.webWeChatGetMsgImg(msg.MsgId, "slave");
                        return wxVideo;
                    }
                    case RspSync.AddMsg.TYPE_EMOJI: {
                        if (XTools.strEmpty(msg.Content) || msg.HasProductId > 0) {
                            //表情商店的表情，无法下载图片
                            WXEmoji wxEmoji = parseCommon(msg, new WXEmoji());
                            wxEmoji.imgWidth = msg.ImgWidth;
                            wxEmoji.imgHeight = msg.ImgHeight;
                            return wxEmoji;
                        } else {
                            //非表情商店的表情，下载图片
                            WXImage wxImage = parseCommon(msg, new WXImage());
                            wxImage.imgWidth = msg.ImgWidth;
                            wxImage.imgHeight = msg.ImgHeight;
                            wxImage.image = wxAPI.webWeChatGetMsgImg(msg.MsgId, "big");
                            wxImage.origin = wxImage.image;
                            return wxImage;
                        }
                    }
                    case RspSync.AddMsg.TYPE_OTHER: {
                        if (msg.AppMsgType == 2) {
                            WXImage wxImage = parseCommon(msg, new WXImage());
                            wxImage.imgWidth = msg.ImgWidth;
                            wxImage.imgHeight = msg.ImgHeight;
                            wxImage.image = wxAPI.webWeChatGetMsgImg(msg.MsgId, "big");
                            wxImage.origin = wxImage.image;
                            return wxImage;
                        } else if (msg.AppMsgType == 5) {
                            WXLink wxLink = parseCommon(msg, new WXLink());
                            wxLink.linkName = msg.FileName;
                            wxLink.linkUrl = msg.Url;
                            return wxLink;
                        } else if (msg.AppMsgType == 6) {
                            WXFile wxFile = parseCommon(msg, new WXFile());
                            wxFile.fileId = msg.MediaId;
                            wxFile.fileName = msg.FileName;
                            wxFile.fileSize = XTools.strEmpty(msg.FileSize) ? 0 : Long.valueOf(msg.FileSize);
                            return wxFile;
                        } else if (msg.AppMsgType == 8) {
                            WXImage wxImage = parseCommon(msg, new WXImage());
                            wxImage.imgWidth = msg.ImgWidth;
                            wxImage.imgHeight = msg.ImgHeight;
                            wxImage.image = wxAPI.webWeChatGetMsgImg(msg.MsgId, "big");
                            wxImage.origin = wxImage.image;
                            return wxImage;
                        } else if (msg.AppMsgType == 2000) {
                            return parseCommon(msg, new WXMoney());
                        }
                        break;
                    }
                    case RspSync.AddMsg.TYPE_NOTIFY: {
                        WXNotify wxNotify = parseCommon(msg, new WXNotify());
                        wxNotify.notifyCode = msg.StatusNotifyCode;
                        wxNotify.notifyContact = msg.StatusNotifyUserName;
                        return wxNotify;
                    }
                    case RspSync.AddMsg.TYPE_SYSTEM: {
                        return parseCommon(msg, new WXSystem());
                    }
                    case RspSync.AddMsg.TYPE_REVOKE:
                        WXRevoke wxRevoke = parseCommon(msg, new WXRevoke());
                        Matcher idMatcher = REX_REVOKE_ID.matcher(wxRevoke.content);
                        if (idMatcher.find()) {
                            wxRevoke.msgId = Long.valueOf(idMatcher.group(1));
                        }
                        Matcher replaceMatcher = REX_REVOKE_REPLACE.matcher(wxRevoke.content);
                        if (replaceMatcher.find()) {
                            wxRevoke.msgReplace = replaceMatcher.group(1);
                        }
                        return wxRevoke;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                XTools.logW(LOG_TAG, "消息解析失败");
            }
            return parseCommon(msg, new WXUnknown());
        }
    }
}
