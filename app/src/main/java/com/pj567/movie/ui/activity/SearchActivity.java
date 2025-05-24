package com.pj567.movie.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.pj567.movie.R;
import com.pj567.movie.api.ApiConfig;
import com.pj567.movie.base.BaseActivity;
import com.pj567.movie.bean.AbsXml;
import com.pj567.movie.bean.Movie;
import com.pj567.movie.bean.SearchRequest;
import com.pj567.movie.event.ServerEvent;
import com.pj567.movie.server.RemoteServer;
import com.pj567.movie.ui.adapter.SearchAdapter;
import com.pj567.movie.util.DefaultConfig;
import com.pj567.movie.util.FastClickCheckUtil;
import com.pj567.movie.util.L;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.tv.QRCodeGen;
import com.tv.leanback.VerticalGridView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class SearchActivity extends BaseActivity {
    private LinearLayout llLayout;
    private VerticalGridView mGridView;
    private TextView tvName;
    private EditText etSearch;
    private TextView tvSearch;
    private TextView tvClear;
    private TextView tvAddress;
    private ImageView ivQRCode;
    private SearchAdapter searchAdapter;
    private int sourceIndex = 0;
    private String searchTitle = "";
    private int sourceTotal = 0;

    public static final String searchPath = "/api.php/provide/vod/?ac=videolist&wd=";
    public static final int maxPages = 3;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_search;
    }

    @Override
    protected void init() {
        initView();
        initViewModel();
        initData();
    }

    private void initView() {
        EventBus.getDefault().register(this);
        llLayout = findViewById(R.id.llLayout);
        etSearch = findViewById(R.id.etSearch);
        tvSearch = findViewById(R.id.tvSearch);
        tvClear = findViewById(R.id.tvClear);
        tvAddress = findViewById(R.id.tvAddress);
        ivQRCode = findViewById(R.id.ivQRCode);
        mGridView = findViewById(R.id.mGridView);
        tvName = findViewById(R.id.tvName);
        mGridView.setHasFixedSize(true);
        mGridView.setNumColumns(1);
        searchAdapter = new SearchAdapter();
        mGridView.setAdapter(searchAdapter);
        searchAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                Movie.Video video = searchAdapter.getData().get(position);
                if (video != null) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", video.id);
                    bundle.putString("sourceUrl", video.api);
                    bundle.putSerializable("data",video);
                    jumpActivity(DetailActivity.class, bundle);
                }
            }
        });
        tvSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                String wd = etSearch.getText().toString().trim();
                if (!TextUtils.isEmpty(wd)) {
                    search(wd);
                } else {
                    Toast.makeText(mContext, "输入内容不能为空", Toast.LENGTH_SHORT).show();
                }
            }
        });
        tvClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                etSearch.setText("");
            }
        });
        setLoadSir(llLayout);
    }

    private void initViewModel() {
    }

    private void initData() {
        refreshQRCode();
        sourceTotal = ApiConfig.get().getSearchRequestList().size();
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("title")) {
            String title = intent.getStringExtra("title");
            showLoading();
            search(title);
        }
    }

    private void refreshQRCode() {
        String address = RemoteServer.getServerAddress(mContext);
        tvAddress.setText(String.format("远程搜索使用手机/电脑扫描下面二维码或者直接浏览器访问地址\n%s", address));
        ivQRCode.setImageBitmap(QRCodeGen.generateBitmap(address, 300, 300));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void server(ServerEvent event) {
        if (event.type == ServerEvent.SERVER_SEARCH) {
            String title = (String) event.obj;
            showLoading();
            search(title);
        }
    }

    private void search(String title) {
        tvName.setText(title);
        sourceIndex = 0;
        cancel();
        showLoading();
        this.searchTitle = title;
        mGridView.setVisibility(View.INVISIBLE);
        searchAdapter.setNewData(new ArrayList<>());
        searchResult();
    }

    private void searchResult() {
        List<SearchRequest> searchRequestList = ApiConfig.get().getSearchRequestList();
        String api = searchRequestList.get(sourceIndex).api;
        String sourceName = searchRequestList.get(sourceIndex).name;
        String apiUrl = api + searchPath + searchTitle;    //翻页用"pg=10"，先不加
        OkGo.<String>get(apiUrl)
                .tag("search")
                .headers("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .headers("Accept","application/json")
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            JSONObject data = new JSONObject(response.body());

                            xml(data,api,sourceName);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            //onApiComplete(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                       /* String errorMsg = "API " + api.getName() + " 搜索失败: " +
                                (response.getException() != null ? response.getException().getMessage() : "未知错误");
                        showToast(errorMsg);
                        onApiComplete(new ArrayList<>());*/
                    }
                });
    }

    private void xml(JSONObject source, String api, String sourceName) {
        try {
            JSONArray list = source.optJSONArray("list");
            AbsXml data = new AbsXml();
            data.movie = new Movie();
            data.movie.page = source.optInt("page");
            data.movie.pagecount = source.optInt("pagecount");
            data.movie.pagesize = source.optInt("total");
            data.movie.recordcount = source.optInt("limit");
            data.movie.videoList = new ArrayList<>();
            data.api = api;
            for (int i = 0; i < list.length(); i++) {
                JSONObject videoJson = list.getJSONObject(i);
                Movie.Video video = new Movie.Video();
                video.last = videoJson.optString("vod_time");
                video.id = videoJson.optInt("vod_id");
                video.tid = videoJson.optInt("type_id");
                video.name = videoJson.optString("vod_name");
                video.type = videoJson.optString("type_name");
                video.dt = videoJson.optString("dt");
                video.pic = videoJson.optString("vod_pic");
                video.lang = videoJson.optString("vod_lang");
                video.area = videoJson.optString("vod_area");
                video.year = videoJson.optInt("vod_year");
                video.state = videoJson.optString("vod_status");
                video.note = videoJson.optString("vod_blurb");
                video.actor = videoJson.optString("vod_actor");
                video.director = videoJson.optString("vod_director");
                video.des = videoJson.optString("des");
                video.api = videoJson.optString("api");
                video.sourceName = videoJson.optString("source_name");
                video.urlBean = new Movie.Video.UrlBean();
                String infoListArray = videoJson.getString("vod_play_url");
                if (!TextUtils.isEmpty(infoListArray)) {
                    video.urlBean.infoList = new ArrayList<>();
                    Movie.Video.UrlBean.UrlInfo urlInfo = new Movie.Video.UrlBean.UrlInfo();
                    urlInfo.flag = "flag";
                    urlInfo.urls = infoListArray;

                    urlInfo.beanList = new ArrayList<>();
                    String[] playSources = urlInfo.urls.split("\\$\\$\\$");

                    // 提取第一个播放源的集数（通常为主要源）
                    if (playSources.length > 0) {
                        String mainSource = playSources[0];
                        String[] episodeList = mainSource.split("#");

                        // 从每个集数中提取URL
                        for (String ep : episodeList) {
                            String[] parts = ep.split("\\$");
                            if (parts.length == 2) {
                                String url = parts[1];
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    urlInfo.beanList.add(new Movie.Video.UrlBean.UrlInfo.InfoBean(parts[0], parts[1]));
                                }
                            }
                        }
                    }

                    video.urlBean.infoList.add(urlInfo);

                }
                video.api = api;
                video.sourceName = sourceName;
                data.movie.videoList.add(video);
            }

            /*if (data.movie != null && data.movie.videoList != null) {
                for (Movie.Video video : data.movie.videoList) {
                    if (video.urlBean != null && video.urlBean.infoList != null) {
                        for (Movie.Video.UrlBean.UrlInfo urlInfo : video.urlBean.infoList) {
                            String[] str = null;
                            if (urlInfo.urls.contains("#")) {
                                str = urlInfo.urls.split("#");
                            } else {
                                str = new String[]{urlInfo.urls};
                            }
                            List<Movie.Video.UrlBean.UrlInfo.InfoBean> infoBeanList = new ArrayList<>();
                            for (String s : str) {
                                if (s.contains("$")) {
                                    infoBeanList.add(new Movie.Video.UrlBean.UrlInfo.InfoBean(s.substring(0, s.indexOf("$")), s.substring(s.indexOf("$") + 1)));
                                }
                            }
                            urlInfo.beanList = infoBeanList;
                        }
                    }
                    video.api = api;
                    video.sourceName = sourceName;
                }
            }*/
            searchData(data);
        } catch (Exception e) {
            searchData(null);
        }
    }

    private void searchData(AbsXml absXml) {
        if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
            List<Movie.Video> data = new ArrayList<>();
            for (Movie.Video video : absXml.movie.videoList) {
                if (!DefaultConfig.isContains(video.type)) {
                    data.add(video);
                }
            }
            if (searchAdapter.getData().size() > 0) {
                searchAdapter.addData(data);
            } else {
                showSuccess();
                mGridView.setVisibility(View.VISIBLE);
                searchAdapter.setNewData(data);
            }
        }
        L.e("sourceIndex = " + sourceIndex);
        if (++sourceIndex == sourceTotal) {
            if (searchAdapter.getData().size() <= 0) {
                showEmpty();
            }
            cancel();
        }else {
            searchResult();
        }
    }

    private void cancel() {
        OkGo.getInstance().cancelTag("search");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancel();
        EventBus.getDefault().unregister(this);
    }
}