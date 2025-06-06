package com.pj567.movie.bean;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

import java.io.Serializable;
import java.util.List;

/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
@XStreamAlias("list")
public class Movie implements Serializable {
    @XStreamAsAttribute
    public int page;
    @XStreamAsAttribute
    public int pagecount;//总页数
    @XStreamAsAttribute
    public int pagesize;
    @XStreamAsAttribute
    public int recordcount;//总条数
    @XStreamImplicit(itemFieldName = "video")
    public List<Video> videoList;

    @XStreamAlias("video")
    public static class Video implements Serializable {
        @XStreamAlias("vod_time")//时间
        public String last;
        @XStreamAlias("vod_id")//内容id
        public int id;
        @XStreamAlias("type_id")//父级id
        public int tid;
        @XStreamAlias("vod_name")//影片名称 <![CDATA[老爸当家]]>
        public String name;
        @XStreamAlias("type_name")//类型名称
        public String type;
        @XStreamAlias("dt")//视频分类 zuidam3u8,zuidall
        public String dt;
        @XStreamAlias("vod_pic")//图片
        public String pic;
        @XStreamAlias("vod_lang")//语言
        public String lang;
        @XStreamAlias("vod_area")//地区
        public String area;
        @XStreamAlias("vod_year")//年份
        public int year;
        @XStreamAlias("vod_status")
        public String state;
        @XStreamAlias("vod_blurb")//描述集数或者影片信息<![CDATA[共40集]]>
        public String note;
        @XStreamAlias("vod_actor")//演员<![CDATA[张国立,蒋欣,高鑫,曹艳艳,王维维,韩丹彤,孟秀,王新]]>
        public String actor;
        @XStreamAlias("vod_director")//导演<![CDATA[陈国星]]>
        public String director;
        @XStreamAlias("vod_play_url")
        public UrlBean urlBean;
        @XStreamAlias("des")
        public String des;// <![CDATA[权来]
        public String api;
        public String sourceName;

        @XStreamAlias("dl")
        public static class UrlBean implements Serializable {
            @XStreamImplicit(itemFieldName = "dd")
            public List<UrlInfo> infoList;

            @XStreamAlias("dd")
            @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"urls"})
            public static class UrlInfo implements Serializable {
                @XStreamAsAttribute
                public String flag;//zuidam3u8,zuidall(MP4)
                // <![CDATA[第01集$http://video.zuidajiexi.com/20170825/txpkmcnK/index.m3u8#第02集$http://video.zuidajiexi.com/20170825/YOApVCHc/index.m3u8]]
                public String urls;
                public List<InfoBean> beanList;

                public static class InfoBean implements Serializable {
                    public String name;
                    public String url;

                    public InfoBean(String name, String url) {
                        this.name = name;
                        this.url = url;
                    }

                }
            }

        }

    }

}