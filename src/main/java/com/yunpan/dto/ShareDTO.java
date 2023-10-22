package com.yunpan.dto;

import lombok.Data;

import java.util.List;

@Data
public class ShareDTO {

    private Integer period;

    private List<Integer> operationList;

    private String password;

    private Integer fileId;

    private Integer shareId;

    private Integer parentId;
}
