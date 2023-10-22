package com.yunpan.controller;

import com.yunpan.dto.ShareDTO;
import com.yunpan.service.ShareService;
import com.yunpan.vo.ShareVo;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/share")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping("/share")
    public ShareVo share(@RequestBody ShareDTO shareDTO, HttpServletRequest request) throws Exception {
        return shareService.share(shareDTO, request);
    }

    @GetMapping("/get_share/{shareId}")
    public ShareVo getShare(@PathVariable("shareId") Integer shareId) throws Exception {
        return shareService.getShare(shareId);
    }

    @PostMapping("/share_files/{shareId}")
    public ShareVo shareFiles(@PathVariable("shareId") Integer shareId, @RequestBody ShareDTO shareDTO) throws Exception {
        return shareService.shareFiles(shareId, shareDTO);
    }

    @PostMapping("/get_share_files")
    public ShareVo getShareFiles(@RequestBody ShareDTO shareDTO) throws Exception {
        return shareService.getShareFiles(shareDTO);
    }

}
