package com.hechuangwu.opensourseframe.rxjava;

import java.util.List;

/**
 * Created by cwh on 2019/9/10 0010.
 * 功能:
 */
public class RxJavaModel {

    /**
     * code : 200
     * msg : 成功!
     * data : {"tech":[{"liveInfo":null,"tcount":1,"picInfo":[{"ref":null,"width":null,"url":"http://cms-bucket.nosdn.127.net/2018/07/13/96e750810c7c47c7b95d6ee112cb9ab4.png","height":null}],"docid":"DMJU4F9E00097U7R","videoInfo":null,"channel":"tech","link":"https://3g.163.com/all/article/DMJU4F9E00097U7R.html","source":"网易科技报道","title":"消息称滴滴考虑明年IPO 放慢融资计划","type":"doc","imgsrcFrom":null,"imgsrc3gtype":1,"unlikeReason":"重复、旧闻/6,内容质量差/6","digest":"网易科技讯7月13日消息，据香港媒体报道，消息人士透露，滴滴","typeid":"","addata":null,"tag":"","category":"科技","ptime":"2018-07-13 15:47:37"}],"auto":[{"liveInfo":null,"tcount":514,"picInfo":[{"ref":null,"width":null,"url":"http://cms-bucket.nosdn.127.net/2018/07/13/bcb246ac4558454d979b593138207f39.jpeg","height":null}],"docid":"DMIP77HO0008856R","videoInfo":null,"channel":"auto","link":"https://3g.163.com/all/article/DMIP77HO0008856R.html","source":"网易汽车","title":"尊贵到令人膨胀 这辆宾利慕尚情怀爆棚","type":"doc","imgsrcFrom":null,"imgsrc3gtype":1,"unlikeReason":"重复、旧闻/6,内容质量差/6","digest":"宾利汽车将于2019年迎来品牌成立100周年，为庆祝这一即将","typeid":"","addata":null,"tag":"","category":"汽车","ptime":"2018-07-13 05:02:30"}]}
     */

    private int code;
    private String msg;
    private DataBean data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        private List<TechBean> tech;
        private List<AutoBean> auto;

        public List<TechBean> getTech() {
            return tech;
        }

        public void setTech(List<TechBean> tech) {
            this.tech = tech;
        }

        public List<AutoBean> getAuto() {
            return auto;
        }

        public void setAuto(List<AutoBean> auto) {
            this.auto = auto;
        }

        public static class TechBean {
            /**
             * liveInfo : null
             * tcount : 1
             * picInfo : [{"ref":null,"width":null,"url":"http://cms-bucket.nosdn.127.net/2018/07/13/96e750810c7c47c7b95d6ee112cb9ab4.png","height":null}]
             * docid : DMJU4F9E00097U7R
             * videoInfo : null
             * channel : tech
             * link : https://3g.163.com/all/article/DMJU4F9E00097U7R.html
             * source : 网易科技报道
             * title : 消息称滴滴考虑明年IPO 放慢融资计划
             * type : doc
             * imgsrcFrom : null
             * imgsrc3gtype : 1
             * unlikeReason : 重复、旧闻/6,内容质量差/6
             * digest : 网易科技讯7月13日消息，据香港媒体报道，消息人士透露，滴滴
             * typeid :
             * addata : null
             * tag :
             * category : 科技
             * ptime : 2018-07-13 15:47:37
             */

            private Object liveInfo;
            private int tcount;
            private String docid;
            private Object videoInfo;
            private String channel;
            private String link;
            private String source;
            private String title;
            private String type;
            private Object imgsrcFrom;
            private int imgsrc3gtype;
            private String unlikeReason;
            private String digest;
            private String typeid;
            private Object addata;
            private String tag;
            private String category;
            private String ptime;
            private List<PicInfoBean> picInfo;

            public Object getLiveInfo() {
                return liveInfo;
            }

            public void setLiveInfo(Object liveInfo) {
                this.liveInfo = liveInfo;
            }

            public int getTcount() {
                return tcount;
            }

            public void setTcount(int tcount) {
                this.tcount = tcount;
            }

            public String getDocid() {
                return docid;
            }

            public void setDocid(String docid) {
                this.docid = docid;
            }

            public Object getVideoInfo() {
                return videoInfo;
            }

            public void setVideoInfo(Object videoInfo) {
                this.videoInfo = videoInfo;
            }

            public String getChannel() {
                return channel;
            }

            public void setChannel(String channel) {
                this.channel = channel;
            }

            public String getLink() {
                return link;
            }

            public void setLink(String link) {
                this.link = link;
            }

            public String getSource() {
                return source;
            }

            public void setSource(String source) {
                this.source = source;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public Object getImgsrcFrom() {
                return imgsrcFrom;
            }

            public void setImgsrcFrom(Object imgsrcFrom) {
                this.imgsrcFrom = imgsrcFrom;
            }

            public int getImgsrc3gtype() {
                return imgsrc3gtype;
            }

            public void setImgsrc3gtype(int imgsrc3gtype) {
                this.imgsrc3gtype = imgsrc3gtype;
            }

            public String getUnlikeReason() {
                return unlikeReason;
            }

            public void setUnlikeReason(String unlikeReason) {
                this.unlikeReason = unlikeReason;
            }

            public String getDigest() {
                return digest;
            }

            public void setDigest(String digest) {
                this.digest = digest;
            }

            public String getTypeid() {
                return typeid;
            }

            public void setTypeid(String typeid) {
                this.typeid = typeid;
            }

            public Object getAddata() {
                return addata;
            }

            public void setAddata(Object addata) {
                this.addata = addata;
            }

            public String getTag() {
                return tag;
            }

            public void setTag(String tag) {
                this.tag = tag;
            }

            public String getCategory() {
                return category;
            }

            public void setCategory(String category) {
                this.category = category;
            }

            public String getPtime() {
                return ptime;
            }

            public void setPtime(String ptime) {
                this.ptime = ptime;
            }

            public List<PicInfoBean> getPicInfo() {
                return picInfo;
            }

            public void setPicInfo(List<PicInfoBean> picInfo) {
                this.picInfo = picInfo;
            }

            public static class PicInfoBean {
                /**
                 * ref : null
                 * width : null
                 * url : http://cms-bucket.nosdn.127.net/2018/07/13/96e750810c7c47c7b95d6ee112cb9ab4.png
                 * height : null
                 */

                private Object ref;
                private Object width;
                private String url;
                private Object height;

                public Object getRef() {
                    return ref;
                }

                public void setRef(Object ref) {
                    this.ref = ref;
                }

                public Object getWidth() {
                    return width;
                }

                public void setWidth(Object width) {
                    this.width = width;
                }

                public String getUrl() {
                    return url;
                }

                public void setUrl(String url) {
                    this.url = url;
                }

                public Object getHeight() {
                    return height;
                }

                public void setHeight(Object height) {
                    this.height = height;
                }
            }
        }

        public static class AutoBean {
            /**
             * liveInfo : null
             * tcount : 514
             * picInfo : [{"ref":null,"width":null,"url":"http://cms-bucket.nosdn.127.net/2018/07/13/bcb246ac4558454d979b593138207f39.jpeg","height":null}]
             * docid : DMIP77HO0008856R
             * videoInfo : null
             * channel : auto
             * link : https://3g.163.com/all/article/DMIP77HO0008856R.html
             * source : 网易汽车
             * title : 尊贵到令人膨胀 这辆宾利慕尚情怀爆棚
             * type : doc
             * imgsrcFrom : null
             * imgsrc3gtype : 1
             * unlikeReason : 重复、旧闻/6,内容质量差/6
             * digest : 宾利汽车将于2019年迎来品牌成立100周年，为庆祝这一即将
             * typeid :
             * addata : null
             * tag :
             * category : 汽车
             * ptime : 2018-07-13 05:02:30
             */

            private Object liveInfo;
            private int tcount;
            private String docid;
            private Object videoInfo;
            private String channel;
            private String link;
            private String source;
            private String title;
            private String type;
            private Object imgsrcFrom;
            private int imgsrc3gtype;
            private String unlikeReason;
            private String digest;
            private String typeid;
            private Object addata;
            private String tag;
            private String category;
            private String ptime;
            private List<PicInfoBeanX> picInfo;

            public Object getLiveInfo() {
                return liveInfo;
            }

            public void setLiveInfo(Object liveInfo) {
                this.liveInfo = liveInfo;
            }

            public int getTcount() {
                return tcount;
            }

            public void setTcount(int tcount) {
                this.tcount = tcount;
            }

            public String getDocid() {
                return docid;
            }

            public void setDocid(String docid) {
                this.docid = docid;
            }

            public Object getVideoInfo() {
                return videoInfo;
            }

            public void setVideoInfo(Object videoInfo) {
                this.videoInfo = videoInfo;
            }

            public String getChannel() {
                return channel;
            }

            public void setChannel(String channel) {
                this.channel = channel;
            }

            public String getLink() {
                return link;
            }

            public void setLink(String link) {
                this.link = link;
            }

            public String getSource() {
                return source;
            }

            public void setSource(String source) {
                this.source = source;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public Object getImgsrcFrom() {
                return imgsrcFrom;
            }

            public void setImgsrcFrom(Object imgsrcFrom) {
                this.imgsrcFrom = imgsrcFrom;
            }

            public int getImgsrc3gtype() {
                return imgsrc3gtype;
            }

            public void setImgsrc3gtype(int imgsrc3gtype) {
                this.imgsrc3gtype = imgsrc3gtype;
            }

            public String getUnlikeReason() {
                return unlikeReason;
            }

            public void setUnlikeReason(String unlikeReason) {
                this.unlikeReason = unlikeReason;
            }

            public String getDigest() {
                return digest;
            }

            public void setDigest(String digest) {
                this.digest = digest;
            }

            public String getTypeid() {
                return typeid;
            }

            public void setTypeid(String typeid) {
                this.typeid = typeid;
            }

            public Object getAddata() {
                return addata;
            }

            public void setAddata(Object addata) {
                this.addata = addata;
            }

            public String getTag() {
                return tag;
            }

            public void setTag(String tag) {
                this.tag = tag;
            }

            public String getCategory() {
                return category;
            }

            public void setCategory(String category) {
                this.category = category;
            }

            public String getPtime() {
                return ptime;
            }

            public void setPtime(String ptime) {
                this.ptime = ptime;
            }

            public List<PicInfoBeanX> getPicInfo() {
                return picInfo;
            }

            public void setPicInfo(List<PicInfoBeanX> picInfo) {
                this.picInfo = picInfo;
            }

            public static class PicInfoBeanX {
                /**
                 * ref : null
                 * width : null
                 * url : http://cms-bucket.nosdn.127.net/2018/07/13/bcb246ac4558454d979b593138207f39.jpeg
                 * height : null
                 */

                private Object ref;
                private Object width;
                private String url;
                private Object height;

                public Object getRef() {
                    return ref;
                }

                public void setRef(Object ref) {
                    this.ref = ref;
                }

                public Object getWidth() {
                    return width;
                }

                public void setWidth(Object width) {
                    this.width = width;
                }

                public String getUrl() {
                    return url;
                }

                public void setUrl(String url) {
                    this.url = url;
                }

                public Object getHeight() {
                    return height;
                }

                public void setHeight(Object height) {
                    this.height = height;
                }
            }
        }
    }
}
