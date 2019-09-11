package com.hechuangwu.opensourseframe.retrofit;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by cwh on 2019/9/4 0004.
 * 功能:
 */
public interface RetrofitService {
    @GET("users/{user}/repos")
    Call<List<String>> listRepos(@Path("user") String user);
}
