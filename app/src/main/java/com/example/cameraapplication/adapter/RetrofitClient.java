package com.example.cameraapplication.adapter;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    static Retrofit retrofit = null;
//    public static String BASEURL = "http://192.168.137.1:5050/camera/camera-api/";
    public static String BASEURL = "http://3.7.19.151:6060/";
    static OkHttpClient client;

    static String token="";

    public static Retrofit getRetrofit(String t){

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        client = new OkHttpClient.Builder()
                .connectTimeout(60*5, TimeUnit.SECONDS)
                .readTimeout(60*5, TimeUnit.SECONDS)
                .writeTimeout(60*5, TimeUnit.SECONDS)
                .addInterceptor(interceptor).build();


        retrofit = new Retrofit.Builder()
                .baseUrl(BASEURL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        token = t;
        return retrofit;
    }
}
