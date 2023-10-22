package com.yunpan.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunpan.config.RSAConfig;
import com.yunpan.entity.User;
import com.yunpan.exception.LoginException;
import com.yunpan.exception.SmsException;
import com.yunpan.exception.UserException;
import com.yunpan.mapper.UserMapper;
import com.yunpan.service.UserService;
import com.yunpan.utils.*;
import com.yunpan.vo.UserVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.yunpan.utils.Constant.*;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final RSAConfig rsaConfig;

    private final RedisTemplate redisTemplate;

    public UserServiceImpl(RSAConfig rsaConfig, RedisTemplate redisTemplate) {
        this.rsaConfig = rsaConfig;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public UserVo login(User user) throws Exception {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        String username = user.getUsername();
        String password = user.getPassword();
        queryWrapper.eq(username != null, User::getUsername, username);
        queryWrapper.eq(password != null, User::getPassword, MD5Utils.md5(password));
        User user1 = this.getOne(queryWrapper);
        if (user1 != null) {
            return getUserVo(user1);
        }
        throw new LoginException();
    }

    @Override
    public String register(User user) throws Exception {
        String phone = user.getPhone();
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user1 = this.getOne(queryWrapper);
        if (user1 != null) {
            throw new SmsException(PHONE_EXISTS_ERROR);
        }
        String username = "user" + ValidateCodeUtils.generateValidateCode4String(6);
        user.setUsername(username);
        user.setPassword(USER_DEFAULT_PASSWORD);
        if (phone == null || "".equals(phone)) {
            throw new SmsException(PHONE_NOT_INPUT_ERROR);
        }
        user.setCreateTime(LocalDateTime.now());
        this.save(user);
        return REGISTER_SUCCESS;
    }

    @Override
    public String checkToken(String token) {
        Map<String, Object> map = JwtUtils.checkToken(token, this.rsaConfig.getPublishStr());
        if (CollUtil.isNotEmpty(map)) {
            return String.valueOf(map.get("userId"));
        }
        return null;
    }

    @Override
    public String sendMsg(User user) throws Exception {
        String phone = user.getPhone();
        boolean isChinaPhoneLegal = PhoneFormatCheckUtils.isChinaPhoneLegal(phone);
        if (!isChinaPhoneLegal) {
            throw new SmsException(PHONE_ERROR);
        }
        String code = (String) redisTemplate.opsForValue().get(phone);
        if (code != null) {
            throw new SmsException(SMS_NOT_FAILURE_ERROR);
        }
        String validateCode = ValidateCodeUtils.generateValidateCode(4).toString();
        SMSUtils.sendSMS(phone, validateCode);
        redisTemplate.opsForValue().set(phone, validateCode, 5, TimeUnit.MINUTES);
        return SMS_SUCCESS;
    }

    @Override
    public UserVo loginBySms(User user, String code) throws Exception {
        String phone = user.getPhone();
        boolean isChinaPhoneLegal = PhoneFormatCheckUtils.isChinaPhoneLegal(phone);
        if (!isChinaPhoneLegal) {
            throw new SmsException(PHONE_ERROR);
        }
        User user1 = this.findByPhone(phone);
        if (user1 != null) {
            String validate = (String) redisTemplate.opsForValue().get(phone);
            if (validate != null && !"".equals(validate) && validate.equals(code)) {
                return getUserVo(user1);
            }
        }
        throw new SmsException(SMS_ERROR);
    }

    @Override
    public User findByPhone(String phone) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(phone != null, User::getPhone, phone);
        return this.getOne(queryWrapper);
    }

    @Override
    public String registerBySms(User user, String code) throws Exception {
        String phone = user.getPhone();
        boolean isChinaPhoneLegal = PhoneFormatCheckUtils.isChinaPhoneLegal(phone);
        if (!isChinaPhoneLegal) {
            throw new SmsException(PHONE_ERROR);
        }
        String validate = (String) redisTemplate.opsForValue().get(phone);
        if (validate != null && !"".equals(validate) && validate.equals(code)) {
            return this.register(user);
        }
        throw new SmsException(SMS_ERROR);
    }

    @Override
    public User updateByUserId(User user) throws Exception {
        Integer userId = UserThreadLocal.get();
        User user1 = this.getById(userId);
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        String username = user.getUsername();
        String password = user.getPassword();

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(username != null && !"".equals(username), User::getUsername, username);
        User one = this.getOne(queryWrapper);
        if (one != null && !userId.equals(one.getId())) {
            throw new UserException(USER_EXISTS_ERROR);
        }

        updateWrapper.eq(User::getId, userId);
        updateWrapper.set(username != null && !"".equals(username), User::getUsername, username);
        updateWrapper.set(password != null && !"".equals(password), User::getPassword, MD5Utils.md5(password));
        this.update(user1, updateWrapper);
        user1 = this.getById(userId);
        return user1;
    }

    private UserVo getUserVo(User user1) {
        user1.setPassword("");
        Map<String, Object> claims = new HashMap<>(16);
        claims.put("userId", USER_REDIS_PREFIX + user1.getId());
        String token = JwtUtils.createToken(claims, this.rsaConfig.getPrivateStr(), 720);
        UserVo userVo = new UserVo();
        userVo.setUser(user1);
        userVo.setToken(token);
        redisTemplate.opsForValue().set(USER_REDIS_PREFIX + user1.getId().toString(), token, 30, TimeUnit.MINUTES);
        return userVo;
    }
}
