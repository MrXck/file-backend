package com.yunpan.controller;

import com.yunpan.dto.FileDTO;
import com.yunpan.service.FileService;
import com.yunpan.vo.FileVo;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/file")
    public FileVo file(@RequestBody FileDTO fileDTO) {
        return fileService.file(fileDTO);
    }

    @GetMapping("/download/{fileId}")
    public void download(@PathVariable("fileId") Integer fileId, HttpServletResponse response) throws Exception {
        fileService.download(fileId, response);
    }

    @PostMapping("/create")
    public FileVo create(@RequestBody FileDTO fileDTO) throws Exception {
        return fileService.create(fileDTO);
    }

    @PostMapping("/delete")
    public FileVo delete(@RequestBody FileDTO fileDTO) {
        return fileService.delete(fileDTO);
    }

    @PostMapping("/search")
    public FileVo search(@RequestBody FileDTO fileDTO) {
        return fileService.search(fileDTO);
    }

    @GetMapping("/rename")
    public FileVo rename(FileDTO fileDTO) throws Exception {
        return fileService.rename(fileDTO);
    }

    @GetMapping("/restore_files")
    public FileVo restore(FileDTO fileDTO) throws Exception {
        return fileService.restore(fileDTO);
    }

    @GetMapping("/download_files")
    public void downloadFiles(FileDTO fileDTO, HttpServletResponse response) {
        fileService.downloadFiles(fileDTO, response);
    }

}



