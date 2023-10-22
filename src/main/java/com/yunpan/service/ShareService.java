package com.yunpan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunpan.dto.ShareDTO;
import com.yunpan.entity.Share;
import com.yunpan.vo.ShareVo;

import javax.servlet.http.HttpServletRequest;

public interface ShareService extends IService<Share> {

    ShareVo share(ShareDTO shareDTO, HttpServletRequest request) throws Exception;

    ShareVo getShare(Integer shareId) throws Exception;

    ShareVo shareFiles(Integer shareId, ShareDTO shareDTO) throws Exception;

    ShareVo getShareFiles(ShareDTO shareDTO) throws Exception;
}
