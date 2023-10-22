package com.yunpan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yunpan.dto.FileDTO;
import com.yunpan.entity.File;
import com.yunpan.exception.FileException;
import com.yunpan.vo.FileVo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface FileService extends IService<File> {
    FileVo file(FileDTO fileDTO);

    void download(Integer fileId, HttpServletResponse response) throws FileException;

    FileVo create(FileDTO fileDTO) throws Exception;

    FileVo delete(FileDTO fileDTO);

    FileVo search(FileDTO fileDTO);

    FileVo rename(FileDTO fileDTO) throws Exception;

    FileVo restore(FileDTO fileDTO) throws Exception;

    void downloadFiles(FileDTO fileDTO, HttpServletResponse response);

    FileVo move(FileDTO fileDTO) throws Exception;

    FileVo dirList(FileDTO fileDTO);

    FileVo dragMove(FileDTO fileDTO) throws Exception;

    FileVo uploadAlready(FileDTO fileDTO) throws Exception;

    FileVo upload(MultipartFile file, FileDTO fileDTO) throws Exception;

    FileVo uploadChunk(Map<String, MultipartFile> map);

    FileVo uploadMerge(FileDTO fileDTO) throws Exception;
}
