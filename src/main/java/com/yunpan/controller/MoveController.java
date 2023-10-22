package com.yunpan.controller;

import com.yunpan.dto.FileDTO;
import com.yunpan.service.FileService;
import com.yunpan.vo.FileVo;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/move")
public class MoveController {

    private final FileService fileService;

    public MoveController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/move")
    public FileVo move(@RequestBody FileDTO fileDTO) throws Exception {
        return fileService.move(fileDTO);
    }

    @PostMapping("/dirList")
    public FileVo dirList(@RequestBody FileDTO fileDTO) {
        return fileService.dirList(fileDTO);
    }

    @GetMapping("/drag_move")
    public FileVo dragMove(FileDTO fileDTO) throws Exception {
        return fileService.dragMove(fileDTO);
    }


}
