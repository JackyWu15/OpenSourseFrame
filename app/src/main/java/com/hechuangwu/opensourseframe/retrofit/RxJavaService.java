package com.hechuangwu.opensourseframe.retrofit;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by cwh on 2019/9/4 0004.
 * 功能:
 */
public interface RxJavaService {
    @GET()
    Observable<ResponseBody> getFileCall(@Url String url);
}
