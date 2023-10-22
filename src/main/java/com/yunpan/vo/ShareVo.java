package com.yunpan.vo;

import com.yunpan.entity.File;
import lombok.Data;

import java.util.List;

@Data
public class ShareVo {

    private Integer code;

    private String url;

    private String password;

    private List<File> data;

    private List<List<Object>> bread;

    private String msg;
}
