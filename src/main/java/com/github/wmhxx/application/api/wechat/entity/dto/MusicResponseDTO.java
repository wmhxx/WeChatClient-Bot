package com.github.wmhxx.application.api.wechat.entity.dto;


public class MusicResponseDTO {



    private boolean success;
    private String sort;
    private InfoBean info;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public InfoBean getInfo() {
        return info;
    }

    public void setInfo(InfoBean info) {
        this.info = info;
    }

    public static class InfoBean {

        private int id;
        private String name;
        private String auther;
        private String picUrl;
        private String mp3url;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAuther() {
            return auther;
        }

        public void setAuther(String auther) {
            this.auther = auther;
        }

        public String getPicUrl() {
            return picUrl;
        }

        public void setPicUrl(String picUrl) {
            this.picUrl = picUrl;
        }

        public String getMp3url() {
            return mp3url;
        }

        public void setMp3url(String mp3url) {
            this.mp3url = mp3url;
        }
    }
}
