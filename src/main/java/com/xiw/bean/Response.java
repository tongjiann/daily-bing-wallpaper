package com.xiw.bean;

import lombok.Data;

import java.util.List;

@Data
public class Response {
    private Market market;

    private List<Image> images;


}
