package com.example.cameraapplication.adapter;

import com.example.cameraapplication.ImageUploader;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RetrofitServices {

    @POST("cbox")
    Call<ImageUploader> uploadImage(@Body ImageUploader imageUploader);
}
