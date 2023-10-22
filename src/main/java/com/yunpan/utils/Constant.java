package com.yunpan.utils;

public class Constant {
    public static Integer DIR_FILE_TYPE = 99;
    public static Integer NOT_DELETE = 0;
    public static Integer DELETED = 1;
    public static Integer NOT_IN_TRASH_STATUS = 1;
    public static Integer IN_TRASH_STATUS = 0;

    public static String USER_REDIS_PREFIX = "user:";

    public static String USER_DEFAULT_PASSWORD = "123456";

    public static String REGISTER_SUCCESS = "注册成功";

    public static String PHONE_EXISTS_ERROR = "手机号已存在";
    public static String PHONE_ERROR = "请输入正确的手机号";
    public static String PHONE_NOT_INPUT_ERROR = "请输入手机号";

    public static String USER_EXISTS_ERROR = "用户名已存在";

    public static String SMS_ERROR = "验证码错误或失效";
    public static String SMS_NOT_FAILURE_ERROR = "短信还未失效, 请稍后重试";
    public static String SMS_SUCCESS = "发送成功";



    public static String LOGIN_ERROR = "用户名或密码错误";

    public static String FILE_NOT_FOUND = "文件不存在";

    public static String UPLOAD_SUCCESS = "上传成功";
    public static String UPLOAD_FILE_ERROR = "该目录下存在相同文件";
    public static String UPLOAD_ERROR = "上传失败";


    public static String CREATE_SUCCESS = "创建成功";
    public static String CREATE_ERROR = "该目录下存在同名文件夹";

    public static String DELETE_SUCCESS = "删除成功";

    public static String RENAME_SUCCESS = "重命名成功";
    public static String RENAME_ERROR = "重命名失败";
    public static String RENAME_FILE_ERROR = "该文件夹下存在相同文件名";
    public static String RENAME_DIR_ERROR = "该文件夹下存在相同目录名";

    public static String RESTORE_SUCCESS = "恢复成功";
    public static String RESTORE_ERROR = "恢复失败";

    public static String MOVE_SUCCESS = "移动成功";
    public static String MOVE_TO_SELF_ERROR = "不可以移动到自己里面";
    public static String MOVE_ERROR = "移动失败";
    public static String MOVE_FILE_ERROR = "该文件夹下存在相同名";

    public static String SHARE_ERROR = "分享失败";
    public static String SHARE_PASSWORD_ERROR = "密码错误";
    public static String SHARE_NOT_IN_VALIDITY_ERROR = "该分享链接已过期";

    public static String SAVE_SUCCESS = "保存成功";
    public static String SAVE_ERROR = "保存失败";
}
