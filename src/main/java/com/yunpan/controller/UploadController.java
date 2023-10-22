package com.yunpan.controller;

import com.yunpan.dto.FileDTO;
import com.yunpan.service.FileService;
import com.yunpan.vo.FileVo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private final FileService fileService;

    public UploadController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/upload_already")
    public FileVo uploadAlready(FileDTO fileDTO) throws Exception {
        return fileService.uploadAlready(fileDTO);
    }

    @PostMapping("/upload")
    public FileVo upload(MultipartFile file, FileDTO fileDTO) throws Exception {
        return fileService.upload(file, fileDTO);
    }

    @PostMapping("/upload_chunk")
    public FileVo uploadChunk(@RequestParam Map<String, MultipartFile> map) {
        return fileService.uploadChunk(map);
    }

    @GetMapping("/upload_merge")
    public FileVo uploadMerge(FileDTO fileDTO) throws Exception {
        return fileService.uploadMerge(fileDTO);
    }

}
