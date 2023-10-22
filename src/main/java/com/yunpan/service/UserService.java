package com.yunpan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunpan.entity.User;
import com.yunpan.vo.UserVo;



public interface UserService extends IService<User> {

    UserVo login(User user) throws Exception;

    String register(User user) throws Exception;

    String checkToken(String token);

    String sendMsg(User user) throws Exception;

    UserVo loginBySms(User user, String code) throws Exception;

    User findByPhone(String phone);

    String registerBySms(User user, String code) throws Exception;

    User updateByUserId(User user) throws Exception;
}
