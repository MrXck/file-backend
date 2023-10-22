package com.yunpan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("web_share_files")
public class ShareFiles {

    private Integer id;

    private Integer shareId;

    private Integer fileId;

}
