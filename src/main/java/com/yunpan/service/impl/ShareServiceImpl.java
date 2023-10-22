package com.yunpan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunpan.dto.ShareDTO;
import com.yunpan.entity.File;
import com.yunpan.entity.Share;
import com.yunpan.entity.ShareFiles;
import com.yunpan.exception.ShareException;
import com.yunpan.mapper.FileMapper;
import com.yunpan.mapper.ShareFilesMapper;
import com.yunpan.mapper.ShareMapper;
import com.yunpan.service.ShareService;
import com.yunpan.utils.UserThreadLocal;
import com.yunpan.utils.ValidateCodeUtils;
import com.yunpan.vo.ShareVo;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.yunpan.utils.Constant.*;

@Service
public class ShareServiceImpl extends ServiceImpl<ShareMapper, Share> implements ShareService {

    private static final Pattern COMPILE = Pattern.compile("[(](\\d+)[)][.]", Pattern.CANON_EQ);

    private final ShareFilesMapper shareFilesMapper;

    private final FileMapper fileMapper;

    public ShareServiceImpl(ShareFilesMapper shareFilesMapper, FileMapper fileMapper) {
        this.shareFilesMapper = shareFilesMapper;
        this.fileMapper = fileMapper;
    }

    @Override
    public ShareVo share(ShareDTO shareDTO, HttpServletRequest request) throws Exception {
        Integer userId = UserThreadLocal.get();
        String password = ValidateCodeUtils.generateValidateCode4String(4);
        Share share = new Share();
        try {
            Integer period = shareDTO.getPeriod();
            List<Integer> operationList = shareDTO.getOperationList();
            share.setCreateTime(LocalDateTime.now());
            share.setCreatorId(userId);
            share.setPassword(password);
            share.setPeriod(period);
            this.save(share);
            for (Integer integer : operationList) {
                ShareFiles shareFiles = new ShareFiles();
                shareFiles.setShareId(share.getId());
                shareFiles.setFileId(Integer.parseInt(integer.toString()));
                shareFilesMapper.insert(shareFiles);
            }
        } catch (Exception e) {
            throw new ShareException(SHARE_ERROR);
        }
        ShareVo shareVo = new ShareVo();
        String url = share.getId().toString();
        shareVo.setCode(0);
        shareVo.setUrl(url);
        shareVo.setPassword(password);
        return shareVo;
    }

    @Override
    public ShareVo getShare(Integer shareId) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Share share = this.getById(shareId);
        if (share == null || share.getCreateTime().plusMinutes(share.getPeriod()).isBefore(now)) {
            throw new ShareException(SHARE_NOT_IN_VALIDITY_ERROR);
        }
        ShareVo shareVo = new ShareVo();
        shareVo.setCode(0);
        return shareVo;
    }

    @Override
    public ShareVo shareFiles(Integer shareId, ShareDTO shareDTO) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Share share = this.getById(shareId);
        if (share == null || share.getCreateTime().plusMinutes(share.getPeriod()).isBefore(now)) {
            throw new ShareException(SHARE_NOT_IN_VALIDITY_ERROR);
        }

        String password = shareDTO.getPassword();
        if (!share.getPassword().equals(password)) {
            throw new ShareException(SHARE_PASSWORD_ERROR);
        }

        List<List<Object>> bread = new ArrayList<>();
        Integer fileId = shareDTO.getFileId();
        List<File> files;
        LambdaQueryWrapper<File> fileLambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (fileId == 0) {
            LambdaQueryWrapper<ShareFiles> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ShareFiles::getShareId, shareId);
            List<ShareFiles> shareFiles = shareFilesMapper.selectList(queryWrapper);
            List<Integer> list = new ArrayList<>();
            for (ShareFiles shareFile : shareFiles) {
                list.add(shareFile.getFileId());
            }
            fileLambdaQueryWrapper.in(File::getId, list);
        } else {
            File file = fileMapper.selectById(fileId);
            if (file != null) {
                List<Object> objectList = new ArrayList<>();
                objectList.add(file.getId());
                objectList.add(file.getFilename());
                bread.add(0, objectList);
                while (file.getParentId() != null) {
                    file = fileMapper.selectById(file.getParentId());
                    objectList = new ArrayList<>();
                    objectList.add(file.getId());
                    objectList.add(file.getFilename());
                    bread.add(0, objectList);
                }
            }
            fileLambdaQueryWrapper.eq(File::getParentId, fileId);
            fileLambdaQueryWrapper.eq(File::getIsDelete, NOT_DELETE);
            fileLambdaQueryWrapper.orderByDesc(File::getFiletype);
        }
        files = fileMapper.selectList(fileLambdaQueryWrapper);

        ShareVo shareVo = new ShareVo();
        shareVo.setCode(0);
        shareVo.setData(files);
        shareVo.setBread(bread);
        return shareVo;
    }

    @Override
    public ShareVo getShareFiles(ShareDTO shareDTO) throws Exception {
        Integer userId = UserThreadLocal.get();
        String password = shareDTO.getPassword();
        Integer shareId = shareDTO.getShareId();
        Share share = this.getById(shareId);
        if (!share.getPassword().equals(password)) {
            throw new ShareException(SHARE_PASSWORD_ERROR);
        }
        try {
            List<Integer> operationList = shareDTO.getOperationList();

            LambdaQueryWrapper<ShareFiles> shareFilesLambdaQueryWrapper = new LambdaQueryWrapper<>();
            shareFilesLambdaQueryWrapper.eq(ShareFiles::getShareId, shareId);
            List<ShareFiles> shareFiles = shareFilesMapper.selectList(shareFilesLambdaQueryWrapper);
            List<Integer> fileIds = shareFiles.stream().map(ShareFiles::getFileId).collect(Collectors.toList());

            for (Integer integer : operationList) {
                if (!fileIds.contains(integer)){
                    throw new ShareException(SAVE_ERROR);
                }
            }

            Integer parentId = shareDTO.getParentId();

            LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(File::getId, operationList);
            queryWrapper.eq(File::getIsDelete, NOT_DELETE);
            queryWrapper.eq(File::getStatus, NOT_IN_TRASH_STATUS);
            List<File> files = fileMapper.selectList(queryWrapper);
            for (File file : files) {
                File file1 = new File();
                LambdaQueryWrapper<File> fileLambdaQueryWrapper = new LambdaQueryWrapper<>();
                file1.setFilename(file.getFilename());
                file1.setFileHashName(file.getFileHashName());
                file1.setFiletype(file.getFiletype());
                file1.setFilepath(file.getFilepath());
                file1.setFileHash(file.getFileHash());
                file1.setStatus(NOT_IN_TRASH_STATUS);
                if (parentId != 0) {
                    file1.setParentId(parentId);
                    fileLambdaQueryWrapper.eq(File::getParentId, parentId);
                } else {
                    fileLambdaQueryWrapper.isNull(File::getParentId);
                }
                file1.setUserId(userId);
                file1.setCreateTime(LocalDateTime.now());
                file1.setIsDelete(NOT_DELETE);

                fileLambdaQueryWrapper.eq(File::getUserId, userId);
                fileLambdaQueryWrapper.eq(File::getFilename, file1.getFilename());
                fileLambdaQueryWrapper.eq(File::getIsDelete, NOT_DELETE);
                Integer count = fileMapper.selectCount(fileLambdaQueryWrapper);
                if (count > 0) {
                    isRepetition(file1, null, userId);
                }
                fileMapper.insert(file1);


                if (DIR_FILE_TYPE.equals(file1.getFiletype())) {
                    LambdaQueryWrapper<File> childrenFileLambdaQueryWrapper = new LambdaQueryWrapper<>();
                    childrenFileLambdaQueryWrapper.eq(File::getParentId, file.getId());
                    childrenFileLambdaQueryWrapper.eq(File::getStatus, NOT_IN_TRASH_STATUS);
                    childrenFileLambdaQueryWrapper.eq(File::getIsDelete, NOT_DELETE);
                    List<File> childrenFiles = fileMapper.selectList(childrenFileLambdaQueryWrapper);
                    getChildrenFiles(userId, childrenFiles, file1.getId());
                }
            }
        } catch (Exception e) {
            throw new ShareException(SAVE_ERROR);
        }
        ShareVo shareVo = new ShareVo();
        shareVo.setCode(0);
        shareVo.setMsg(SAVE_SUCCESS);
        return shareVo;
    }

    private void getChildrenFiles(Integer userId, List<File> childrenFiles, Integer id) {
        for (File childrenFile : childrenFiles) {
            Integer parentId = saveFile(childrenFile.getFilename(),
                    id,
                    childrenFile.getFileHash(),
                    childrenFile.getFiletype().toString(),
                    userId,
                    childrenFile.getFileHashName(),
                    childrenFile.getFilepath()
            );
            if (DIR_FILE_TYPE.equals(childrenFile.getFiletype())) {
                LambdaQueryWrapper<File> childrenFileLambdaQueryWrapper = new LambdaQueryWrapper<>();
                childrenFileLambdaQueryWrapper.eq(File::getParentId, childrenFile.getId());
                childrenFileLambdaQueryWrapper.eq(File::getStatus, NOT_IN_TRASH_STATUS);
                childrenFileLambdaQueryWrapper.eq(File::getIsDelete, NOT_DELETE);
                childrenFiles = fileMapper.selectList(childrenFileLambdaQueryWrapper);
                getChildrenFiles(userId, childrenFiles, parentId);
            }
        }
    }


    public void isRepetition(File restoreFile, File file, Integer userId) {
        if (file != null) {
            Matcher matcher = COMPILE.matcher(file.getFilename());
            if (matcher.find()) {
                String group = matcher.group();
                String group1 = matcher.group(1);
                String group2 = group.replace(group1, String.valueOf(Integer.parseInt(group1) + 1));
                restoreFile.setFilename(file.getFilename().replace(group, group2));
            } else {
                int i = file.getFilename().lastIndexOf('.');
                if (i == -1) {
                    restoreFile.setFilename(file.getFilename() + "(1).dir");
                } else {
                    String[] split = file.getFilename().split("\\.");
                    restoreFile.setFilename(split[split.length - 2] + "(1)." + split[split.length - 1]);
                }
            }
        }
        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(File::getUserId, userId);
        queryWrapper.eq(File::getFilename, restoreFile.getFilename());
        Integer parentId = restoreFile.getParentId();
        if (parentId == null) {
            queryWrapper.isNull(File::getParentId);
        } else {
            queryWrapper.eq(File::getParentId, restoreFile.getParentId());
        }
        queryWrapper.eq(File::getIsDelete, NOT_DELETE);
        File file1 = fileMapper.selectOne(queryWrapper);
        if (null != file1) {
            isRepetition(restoreFile, file1, userId);
        }
    }

    private Integer saveFile(String filename, Integer parentId, String fileHash, String filetype, Integer userId, String uuidName, String filePath) {
        File file = new File();
        file.setFilename(filename);
        file.setFileHashName(uuidName);
        file.setFiletype(Integer.parseInt(filetype));
        file.setFilepath(filePath);
        file.setFileHash(fileHash);
        file.setStatus(NOT_IN_TRASH_STATUS);
        if (parentId != 0) {
            file.setParentId(parentId);
        }
        file.setUserId(userId);
        file.setCreateTime(LocalDateTime.now());
        file.setIsDelete(NOT_DELETE);
        fileMapper.insert(file);
        return file.getId();
    }

}
