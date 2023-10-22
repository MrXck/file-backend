package com.yunpan.interceptor;

import com.alibaba.druid.util.StringUtils;
import com.yunpan.service.UserService;
import com.yunpan.utils.NoAuthorization;
import com.yunpan.utils.UserThreadLocal;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * 用于统一校验token的有效性，如果token有效就将userId存储到本地线程中，否则响应401
 *
 * @author xck
 */
@Component
public class TokenInterceptor implements HandlerInterceptor {

    private final UserService userService;

    private final RedisTemplate redisTemplate;

    public TokenInterceptor(UserService userService, RedisTemplate redisTemplate) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            // 没有匹配到Controller中的方法
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        //判断该方法是否是@NoAuthorization请求
        if (handlerMethod.hasMethodAnnotation(NoAuthorization.class)) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (!StringUtils.isEmpty(token)) {
            String[] split = this.userService.checkToken(token).split(":");
            String userType = split[0];
            Integer userId = Integer.parseInt(split[1]);
            String redisToken = (String) redisTemplate.opsForValue().get(userType + ":" + userId.toString());
            if (redisToken != null && !"".equals(redisToken)) {
                //把id放入本地线程中
                UserThreadLocal.set(userId);
                redisTemplate.opsForValue().set(userType + ":" + userId.toString(), token, 30, TimeUnit.MINUTES);
                return true;
            }
        }
        //给客户端响应401状态码
        response.setStatus(401);

        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //把本地线程中的用户id删除
        UserThreadLocal.remove();
    }

}
