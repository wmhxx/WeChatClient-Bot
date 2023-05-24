package com.github.wmhxx.application.api.wechat.entity.dto;


import cn.hutool.json.JSONUtil;
import com.github.wmhxx.application.api.wechat.entity.message.WxMsg;

import java.util.List;

public class HotResponseDTO {


    private boolean success;
    private String title;
    private String subtitle;
    private String update_time;
    private List<DataBean> data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(String update_time) {
        this.update_time = update_time;
    }

    public List<DataBean> getData() {
        return data;
    }

    public void setData(List<DataBean> data) {
        this.data = data;
    }

    public static class DataBean {

        private int index;
        private String title;
        private String hot;
        private String url;
        private String mobilUrl;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getHot() {
            return hot;
        }

        public void setHot(String hot) {
            this.hot = hot;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMobilUrl() {
            return mobilUrl;
        }

        public void setMobilUrl(String mobilUrl) {
            this.mobilUrl = mobilUrl;
        }
    }


    public static String getResponse(String text){
        HotResponseDTO hotResponseDTO = JSONUtil.toBean(text, HotResponseDTO.class);
        StringBuilder responseText = new StringBuilder();
        int i = 1;
        for (HotResponseDTO.DataBean datum : hotResponseDTO.getData()) {
            if(i == 20){
                break;
            }
            responseText.append("\uD83D\uDD25").append(datum.getHot());
            responseText.append("【").append(datum.getIndex()).append("】");
            responseText.append(datum.getTitle());
            responseText.append(WxMsg.LINE);
            i++;
        }
        return responseText.toString();
    }
}
