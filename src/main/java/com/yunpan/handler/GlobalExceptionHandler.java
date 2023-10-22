package com.yunpan.handler;

import com.yunpan.common.R;
import com.yunpan.exception.*;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

import static com.yunpan.utils.Constant.FILE_NOT_FOUND;
import static com.yunpan.utils.Constant.LOGIN_ERROR;

/**
 * @author xck
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IOException.class)
    public R<String> handleBindException(IOException ex) {
        return R.error(FILE_NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public R<String> handleBindException(Exception ex) {
        ex.printStackTrace();
        return R.error("操作失败");
    }

    @ExceptionHandler(LoginException.class)
    public R<String> handleBindException(LoginException ex) {
        return R.error(LOGIN_ERROR);
    }

    @ExceptionHandler(RenameException.class)
    public R<String> handleBindException(RenameException ex) {
        return R.error(ex.getMessage());
    }

    @ExceptionHandler(RestoreException.class)
    public R<String> handleBindException(RestoreException ex) {
        return R.error(ex.getMessage());
    }

    @ExceptionHandler(MoveException.class)
    public R<String> handleBindException(MoveException ex) {
        return R.error(ex.getMessage());
    }

    @ExceptionHandler(ShareException.class)
    public R<String> handleBindException(ShareException ex) {
        return R.error(ex.getMessage());
    }

    @ExceptionHandler(FileException.class)
    public R<String> handleBindException(FileException ex) {
        return R.error(ex.getMessage());
    }

    @ExceptionHandler(SmsException.class)
    public R<String> handleBindException(SmsException ex) {
        return R.error(ex.getMessage());
    }

    @ExceptionHandler(UserException.class)
    public R<String> handleBindException(UserException ex) {
        return R.error(ex.getMessage());
    }

    @ExceptionHandler(FileUploadException.class)
    public R<String> handleBindException(FileUploadException ex) {
        return R.error(ex.getMessage());
    }

}
