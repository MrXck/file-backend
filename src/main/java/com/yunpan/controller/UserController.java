package com.yunpan.controller;

import com.yunpan.common.R;
import com.yunpan.entity.User;
import com.yunpan.service.UserService;
import com.yunpan.utils.NoAuthorization;
import com.yunpan.vo.UserVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @NoAuthorization
    @PostMapping("login")
    private UserVo login(@RequestBody User user) throws Exception {
        return userService.login(user);
    }

    @NoAuthorization
    @PostMapping("/register")
    private R<String> register(@RequestBody User user, String code) throws Exception {
        return R.success(userService.registerBySms(user, code));
    }

    @NoAuthorization
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user) throws Exception {
        return R.success(userService.sendMsg(user));
    }

    @NoAuthorization
    @PostMapping("/loginBySms")
    public UserVo loginBySms(@RequestBody User user, String code) throws Exception {
        return userService.loginBySms(user, code);
    }

    @PostMapping("/update")
    public R<User> update(@RequestBody User user) throws Exception {
        User user1 = userService.updateByUserId(user);
        return R.success(user1);
    }
}
