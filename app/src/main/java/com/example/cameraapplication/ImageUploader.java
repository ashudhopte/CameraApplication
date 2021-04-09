package com.example.cameraapplication;

import java.io.Serializable;

public class ImageUploader implements Serializable {

    Integer imageId;
    String inputUrl;
    String outputUrl;


    public String getInputUrl() {
        return inputUrl;
    }

    public void setInputUrl(String inputUrl) {
        this.inputUrl = inputUrl;
    }

    public String getOutputUrl() {
        return outputUrl;
    }

    public void setOutputUrl(String outputUrl) {
        this.outputUrl = outputUrl;
    }

    public Integer getImageId() {
        return imageId;
    }

    public void setImageId(Integer imageId) {
        this.imageId = imageId;
    }
}
