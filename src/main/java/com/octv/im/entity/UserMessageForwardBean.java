package com.octv.im.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserMessageForwardBean implements Serializable {
    private String userId;

    private String url;

    private String cameraType;

    private String route;
}
