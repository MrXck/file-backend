package com.yunpan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("web_file")
public class File {

    private Integer id;

    private String filename;

    private String fileHashName;

    private Integer filetype;

    private String filepath;

    private String fileHash;

    private Integer status;

    private LocalDateTime createTime;

    private Integer isDelete;

    private Integer parentId;

    private Integer userId;
}
