package com.yunpan.vo;

import com.yunpan.entity.File;
import lombok.Data;

import java.util.List;

@Data
public class FileVo {

    private Integer code;

    private List<File> data;

    private List<List<Object>> bread;

    private String msg;

    private List<Object> fileList;
}
