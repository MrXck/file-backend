package com.yunpan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("web_share")
public class Share {

    private Integer id;

    private LocalDateTime createTime;

    private Integer period;

    private String password;

    private Integer creatorId;
}
