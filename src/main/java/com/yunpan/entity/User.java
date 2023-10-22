package com.yunpan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("web_user")
@Data
public class User {

    private Integer id;

    private String username;

    private String password;

    private String phone;

    private LocalDateTime createTime;
}
