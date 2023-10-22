package com.yunpan.dto;

import lombok.Data;

import java.util.List;

@Data
public class FileDTO {

    private Integer fileId;

    private Integer status;

    private String dirName;

    private List<Integer> operationList;

    private String query;

    private String rename;

    private Integer parentId;

    private String HASH;

    private String filename;

    private String fileHash;

    private String filetype;

    private Integer count;
}
