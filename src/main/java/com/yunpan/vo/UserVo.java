package com.yunpan.vo;


import com.yunpan.entity.User;
import lombok.Data;

@Data
public class UserVo {

    private User user;

    private String token;
}
