package com.yunpan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunpan.dto.FileDTO;
import com.yunpan.entity.File;
import com.yunpan.exception.FileException;
import com.yunpan.exception.MoveException;
import com.yunpan.exception.RenameException;
import com.yunpan.exception.RestoreException;
import com.yunpan.mapper.FileMapper;
import com.yunpan.service.FileService;
import com.yunpan.utils.UserThreadLocal;
import com.yunpan.utils.ZipUtils;
import com.yunpan.vo.FileVo;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.yunpan.utils.Constant.*;

@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    private static final Pattern COMPILE = Pattern.compile("\\((\\d+)\\)\\.", Pattern.CANON_EQ);

    private static final String PATH = System.getProperty("user.dir") + "/files/";

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public FileVo file(FileDTO fileDTO) {
        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        Integer fileId = fileDTO.getFileId();
        Integer status = fileDTO.getStatus();
        Integer userId = UserThreadLocal.get();
        List<List<Object>> bread = new ArrayList<>();
        if (fileId != 0) {
            queryWrapper.eq(File::getParentId, fileId);
        } else {
            queryWrapper.isNull(File::getParentId);
        }
        queryWrapper.eq(File::getStatus, status);
        queryWrapper.eq(File::getUserId, userId);
        queryWrapper.eq(File::getIsDelete, NOT_DELETE);
        queryWrapper.orderByDesc(File::getFiletype);
        List<File> files = this.list(queryWrapper);
        getBread(fileId, bread);
        FileVo fileVo = new FileVo();
        fileVo.setCode(0);
        fileVo.setData(files);
        fileVo.setBread(bread);
        return fileVo;
    }

    @Override
    public void download(Integer fileId, HttpServletResponse response) throws FileException {
        Integer userId = UserThreadLocal.get();
        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(File::getUserId, userId);
        queryWrapper.eq(File::getId, fileId);
        File file = this.getOne(queryWrapper);
        try (FileInputStream fileInputStream = new FileInputStream(file.getFilepath().replace("\u202a", ""))) {
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType("application/octet-stream");
            int len;
            byte[] bytes = new byte[1024];
            while ((len = fileInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, len);
                outputStream.flush();
            }
            outputStream.close();
        } catch (Exception e) {
            throw new FileException(FILE_NOT_FOUND);
        }
    }

    @Override
    public FileVo create(FileDTO fileDTO) throws Exception {
        Integer userId = UserThreadLocal.get();

        Integer fileId = fileDTO.getFileId();
        String dirName = fileDTO.getDirName();
        FileVo fileVo = new FileVo();


        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        if (fileId == 0) {
            queryWrapper.isNull(File::getParentId);
        } else {
            queryWrapper.eq(File::getParentId, fileId);
        }

        queryWrapper.eq(File::getUserId, userId);
        queryWrapper.eq(File::getFilename, dirName);
        queryWrapper.eq(File::getFiletype, DIR_FILE_TYPE);
        queryWrapper.eq(File::getStatus, NOT_IN_TRASH_STATUS);
        queryWrapper.eq(File::getIsDelete, NOT_DELETE);

        int count = this.count(queryWrapper);
        if (count == 0) {
            saveFile(dirName, fileId, "1", DIR_FILE_TYPE.toString(), userId, "1", "1");
            fileVo.setCode(0);
            fileVo.setMsg(CREATE_SUCCESS);
            return fileVo;
        }
        throw new FileException(CREATE_ERROR);
    }

    @Override
    public FileVo delete(FileDTO fileDTO) {
        Integer userId = UserThreadLocal.get();
        FileVo fileVo = new FileVo();
        List<Integer> operationList = fileDTO.getOperationList();
        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(File::getUserId, userId);
        queryWrapper.eq(File::getIsDelete, NOT_DELETE);
        queryWrapper.in(File::getId, operationList);
        List<File> files = this.list(queryWrapper);
        for (File file : files) {
            deleteChild(file, userId);
        }
        fileVo.setCode(0);
        fileVo.setMsg(DELETE_SUCCESS);
        return fileVo;
    }

    @Override
    public FileVo search(FileDTO fileDTO) {
        Integer userId = UserThreadLocal.get();
        String query = fileDTO.getQuery();
        FileVo fileVo = new FileVo();
        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(File::getFilename, query);
        queryWrapper.eq(File::getUserId, userId);
        queryWrapper.eq(File::getStatus, NOT_IN_TRASH_STATUS);
        queryWrapper.eq(File::getIsDelete, NOT_DELETE);
        List<File> files = this.list(queryWrapper);
        fileVo.setCode(0);
        fileVo.setData(files);
        return fileVo;
    }

    @Override
    public FileVo rename(FileDTO fileDTO) throws Exception {
        Integer userId = UserThreadLocal.get();
        List<Integer> operationList = fileDTO.getOperationList();
        String rename = fileDTO.getRename();
        FileVo fileVo = new FileVo();
        Integer parentId = fileDTO.getParentId();
        if (null == rename && operationList.size() != 1) {
            throw new RenameException(RENAME_ERROR);
        }

        LambdaQueryWrapper<File> fileQueryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<File> dirQueryWrapper = new LambdaQueryWrapper<>();
        if (parentId == 0) {
            fileQueryWrapper.isNull(File::getParentId);
            dirQueryWrapper.isNull(File::getParentId);
        } else {
            fileQueryWrapper.eq(File::getParentId, parentId);
            dirQueryWrapper.eq(File::getParentId, parentId);
        }

        fileQueryWrapper.eq(File::getUserId, userId);
        fileQueryWrapper.ne(File::getFiletype, DIR_FILE_TYPE);
        fileQueryWrapper.eq(File::getStatus, NOT_IN_TRASH_STATUS);
        fileQueryWrapper.eq(File::getIsDelete, NOT_DELETE);
        List<File> files = this.list(fileQueryWrapper);
        boolean flag = true;
        for (File file : files) {
            if (rename != null && rename.equals(file.getFilename())) {
                flag = false;
                break;
            }
        }
        if (flag) {
            if (renameFile(operationList, rename, fileVo, fileQueryWrapper)) {
                return fileVo;
            }
        } else {
            throw new RenameException(RENAME_FILE_ERROR);
        }


        dirQueryWrapper.eq(File::getUserId, userId);
        dirQueryWrapper.eq(File::getFiletype, DIR_FILE_TYPE);
        dirQueryWrapper.eq(File::getStatus, NOT_IN_TRASH_STATUS);
        dirQueryWrapper.eq(File::getIsDelete, NOT_DELETE);
        List<File> dirs = this.list(dirQueryWrapper);
        for (File dir : dirs) {
            if (rename != null && rename.equals(dir.getFilename())) {
                flag = false;
                break;
            }
        }
        if (flag) {
            if (renameFile(operationList, rename, fileVo, dirQueryWrapper)) {
                return fileVo;
            }
        } else {
            throw new RenameException(RENAME_DIR_ERROR);
        }

        fileVo.setCode(0);
        fileVo.setMsg(RENAME_SUCCESS);
        return fileVo;
    }


    @Override
    public FileVo restore(FileDTO fileDTO) throws Exception {
        List<Integer> operationList = fileDTO.getOperationList();
        Integer userId = UserThreadLocal.get();

        if (operationList.isEmpty()) {
            throw new RestoreException(RESTORE_ERROR);
        }

        FileVo fileVo = new FileVo();

        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(File::getId, operationList);
        queryWrapper.eq(File::getUserId, userId);
        queryWrapper.eq(File::getStatus, IN_TRASH_STATUS);
        queryWrapper.eq(File::getIsDelete, NOT_DELETE);
        List<File> files = this.list(queryWrapper);
        restoreChild(files, userId);
        fileVo.setCode(0);
        fileVo.setMsg(RESTORE_SUCCESS);
        return fileVo;
    }

    @Override
    public void downloadFiles(FileDTO fileDTO, HttpServletResponse response) {
        Integer userId = UserThreadLocal.get();
        List<Integer> operationList = fileDTO.getOperationList();
        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(File::getUserId, userId);
        queryWrapper.eq(File::getStatus, NOT_IN_TRASH_STATUS);
        queryWrapper.in(File::getId, operationList);
        List<File> files = this.list(queryWrapper);
        List<File> fileList = new ArrayList<>();
        for (File file : files) {
            if (!DIR_FILE_TYPE.equals(file.getFiletype())) {
                fileList.add(file);
            } else {
                LambdaQueryWrapper<File> fileLambdaQueryWrapper = new LambdaQueryWrapper<>();
                fileLambdaQueryWrapper.eq(File::getUserId, userId);
                fileLambdaQueryWrapper.eq(File::getStatus, NOT_IN_TRASH_STATUS);
                fileLambdaQueryWrapper.eq(File::getParentId, file.getId());
                List<File> list = this.list(fileLambdaQueryWrapper);
                getFiles(userId, fileList, list);
            }
        }
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=");
        try {
            ZipUtils.downloadZip(response.getOutputStream(), fileList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public FileVo move(FileDTO fileDTO) throws Exception {
        Integer userId = UserThreadLocal.get();
        List<Integer> operationList = fileDTO.getOperationList();
        Integer parentId = fileDTO.getParentId();
        moveFiles(userId, operationList, parentId);
        FileVo fileVo = new FileVo();
        fileVo.setCode(0);
        fileVo.setMsg(MOVE_SUCCESS);
        return fileVo;
    }

    @Override
    public FileVo dirList(FileDTO fileDTO) {
        Integer userId = UserThreadLocal.get();
        Integer parentId = fileDTO.getParentId();
        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        List<File> files;
        List<List<Object>> bread = new ArrayList<>();
        queryWrapper.eq(File::getUserId, userId);
        queryWrapper.eq(File::getFiletype, DIR_FILE_TYPE);
        queryWrapper.eq(File::getStatus, NOT_IN_TRASH_STATUS);
        queryWrapper.eq(File::getIsDelete, NOT_DELETE);
        if (parentId == 0) {
            queryWrapper.isNull(File::getParentId);
        } else {
            getBread(parentId, bread);
            queryWrapper.eq(File::getParentId, parentId);
        }
        files = this.list(queryWrapper);
        FileVo fileVo = new FileVo();
        fileVo.setCode(0);
        fileVo.setData(files);
        fileVo.setBread(bread);
        return fileVo;
    }

    @Override
    public FileVo dragMove(FileDTO fileDTO) throws Exception {
        Integer userId = UserThreadLocal.get();
        List<Integer> operationList = fileDTO.getOperationList();
        Integer parentId = fileDTO.getParentId();
        if (operationList.isEmpty()) {
            throw new MoveException(MOVE_ERROR);
        }
        if (operationList.contains(parentId)) {
            throw new MoveException(MOVE_TO_SELF_ERROR);
        }
        moveFiles(userId, operationList, parentId);
        FileVo fileVo = new FileVo();
        fileVo.setCode(0);
        fileVo.setMsg(MOVE_SUCCESS);
        return fileVo;
    }

    @Override
    public FileVo uploadAlready(FileDTO fileDTO) throws Exception {
        Integer userId = UserThreadLocal.get();
        FileVo fileVo = new FileVo();
        String hash = fileDTO.getHASH();
        String filename = fileDTO.getFilename();
        String fileHash = fileDTO.getFileHash();
        Integer parentId = fileDTO.getParentId();
        String[] fullPath = filename.split("/");
        if (fullPath.length > 1) {
            parentId = isExists(userId, parentId, fullPath);
        }

        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        if (parentId == 0) {
            queryWrapper.isNull(File::getParentId);
        } else if (parentId != -1) {
            queryWrapper.eq(File::getParentId, parentId);
        } else {
            fileVo.setCode(0);
            return fileVo;
        }
        queryWrapper.eq(File::getUserId, userId);
        queryWrapper.eq(File::getFilename, fullPath[fullPath.length - 1]);
        queryWrapper.ne(File::getFiletype, DIR_FILE_TYPE);
        queryWrapper.eq(File::getStatus, NOT_IN_TRASH_STATUS);
        queryWrapper.eq(File::getIsDelete, NOT_DELETE);
        File one = this.getOne(queryWrapper);
        if (one != null) {
            throw new FileUploadException(UPLOAD_FILE_ERROR);
        }
        LambdaQueryWrapper<File> fileQueryWrapper = new LambdaQueryWrapper<>();
        fileQueryWrapper.ne(File::getFiletype, DIR_FILE_TYPE);
        fileQueryWrapper.eq(File::getFileHash, fileHash);
        List<File> fileList = this.list(fileQueryWrapper);
        if (!fileList.isEmpty()) {
            File file = fileList.get(0);
            saveFile(fullPath[fullPath.length - 1], parentId, file.getFileHash(), file.getFiletype().toString(), userId, file.getFileHashName(), file.getFilepath());
            fileVo.setCode(2);
            fileVo.setMsg(UPLOAD_SUCCESS);
            return fileVo;
        }
        if (null == hash) {
            fileVo.setCode(0);
            return fileVo;
        }
        String dirPath = PATH + "/" + hash;
        java.io.File file = new java.io.File(dirPath);
        List<Object> list = new ArrayList<>();
        if (!file.exists()) {
            file.mkdirs();
        } else {
            java.io.File[] files1 = file.listFiles();
            if (files1 != null) {
                for (java.io.File file1 : files1) {
                    list.add(file1.getName());
                }
            }
        }
        fileVo.setCode(0);
        fileVo.setFileList(list);
        return fileVo;
    }

    @Override
    public FileVo upload(MultipartFile file, FileDTO fileDTO) throws Exception {
        Integer userId = UserThreadLocal.get();
        FileVo fileVo = new FileVo();
        String hash = String.valueOf(UUID.randomUUID());
        String fileHash = fileDTO.getFileHash();
        Integer parentId = fileDTO.getParentId();
        String filetype = fileDTO.getFiletype();
        String filename = fileDTO.getFilename();
        String[] fullPath = filename.split("/");

        parentId = isExistFile(fullPath, file.getOriginalFilename(), userId, parentId);

        String[] split = Objects.requireNonNull(file.getOriginalFilename()).split("\\.");
        String suffix = split[split.length - 1];
        String path = PATH + "/" + hash + "." + suffix;
        saveFile(file.getOriginalFilename(), parentId, fileHash, filetype, userId, hash, path);
        java.io.File f = new java.io.File(path);
        if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
        try {
            file.transferTo(f);
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileUploadException(UPLOAD_ERROR);
        }
        fileVo.setCode(0);
        fileVo.setMsg(UPLOAD_SUCCESS);
        return fileVo;
    }

    @Override
    public FileVo uploadChunk(Map<String, MultipartFile> map) {
        String filename = (String) map.keySet().toArray()[0];
        String dirPath = PATH + filename.split("_")[0];
        java.io.File file1 = new java.io.File(dirPath);
        try {
            if (!file1.exists()) {
                file1.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String filePath = dirPath + "/" + filename;
        MultipartFile multipartFile = map.get(filename);
        try {
            multipartFile.transferTo(new java.io.File(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileVo fileVo = new FileVo();
        fileVo.setCode(0);
        return fileVo;
    }

    @Override
    public FileVo uploadMerge(FileDTO fileDTO) throws Exception {
        Integer userId = UserThreadLocal.get();
        String hash = fileDTO.getHASH();
        Integer count = fileDTO.getCount();
        Integer parentId = fileDTO.getParentId();
        String filename = fileDTO.getFilename();
        String[] fullPath = filename.split("/");
        parentId = isExistFile(fullPath, fullPath[fullPath.length - 1], userId, parentId);
        String fileHash = fileDTO.getFileHash();
        String filetype = fileDTO.getFiletype();
        String uuidName = String.valueOf(UUID.randomUUID());
        String fileHashPath = PATH + "/" + hash;
        String[] split = filename.split("\\.");
        String filePath = PATH + "/" + uuidName + "." + split[split.length - 1];
        java.io.File[] files = new java.io.File(fileHashPath).listFiles();
        assert files != null;
        String[] split1 = files[0].getName().split("\\.");
        String suffix = "." + split1[split1.length - 1];
        FileInputStream fileInputStream = null;
        try {
            new java.io.File(filePath).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
            for (int i = 1; i < count + 1; i++) {
                String mergeFilename = hash + "_" + i + suffix;
                String mergeFilepath = fileHashPath + "/" + mergeFilename;
                fileInputStream = new FileInputStream(mergeFilepath);
                byte[] buf = new byte[1024];
                int len;
                while ((len = fileInputStream.read(buf)) != -1) {
                    fileOutputStream.write(buf, 0, len);
                    fileOutputStream.flush();
                }
                fileInputStream.close();
                new java.io.File(mergeFilepath).delete();
            }
            new java.io.File(fileHashPath).delete();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        saveFile(fullPath[fullPath.length - 1], parentId, fileHash, filetype, userId, uuidName, filePath);

        FileVo fileVo = new FileVo();
        fileVo.setCode(0);
        fileVo.setMsg(UPLOAD_SUCCESS);
        return fileVo;
    }

    private Integer isExists(Integer userId, Integer parentId, String[] fullPath) {
        lock.lock();
        try {
            for (int i = 0; i < fullPath.length; i++) {
                if (i != fullPath.length - 1) {
                    File parent = queryParent(userId, parentId, fullPath, i);
                    if (parent == null) {
                        return -1;
                    } else {
                        parentId = parent.getId();
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        return parentId;
    }

    private Integer createParent(Integer userId, Integer parentId, String[] fullPath) throws Exception {
        lock.lock();
        try {
            for (int i = 0; i < fullPath.length; i++) {
                if (i != fullPath.length - 1) {
                    File parent = queryParent(userId, parentId, fullPath, i);
                    if (parent == null) {
                        parentId = saveFile(fullPath[i], parentId, "1", DIR_FILE_TYPE.toString(), userId, "1", "1");
                    } else {
                        parentId = parent.getId();
                    }
                }
            }
        } catch (Exception e) {
            throw new FileException(UPLOAD_ERROR);
        } finally {
            lock.unlock();
        }
        return parentId;
    }

    private File queryParent(Integer userId, Integer parentId, String[] fullPath, int i) {
        LambdaQueryWrapper<File> fileLambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (parentId == 0) {
            fileLambdaQueryWrapper.isNull(File::getParentId);
        } else {
            fileLambdaQueryWrapper.eq(File::getParentId, parentId);
        }
        fileLambdaQueryWrapper.eq(File::getFilename, fullPath[i]);
        fileLambdaQueryWrapper.eq(File::getFiletype, DIR_FILE_TYPE);
        fileLambdaQueryWrapper.eq(File::getUserId, userId);
        return this.getOne(fileLambdaQueryWrapper);
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
        this.save(file);
        return file.getId();
    }

    public void restoreChild(List<File> files, Integer userId) {
        for (File file : files) {
            LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(File::getUserId, userId);
            if (file.getParentId() != null) {
                queryWrapper.eq(File::getParentId, file.getParentId());
            } else {
                queryWrapper.isNull(File::getParentId);
            }
            queryWrapper.eq(File::getFilename, file.getFilename());
            queryWrapper.eq(File::getIsDelete, NOT_DELETE);
            queryWrapper.ne(File::getId, file.getId());
            List<File> fileObjList = this.list(queryWrapper);
            if (!fileObjList.isEmpty()) {
                isRepetition(file, new File(), userId);
            }
            file.setStatus(NOT_IN_TRASH_STATUS);
            this.updateById(file);
            LambdaQueryWrapper<File> fileQueryWrapper = new LambdaQueryWrapper<>();
            fileQueryWrapper.eq(File::getUserId, userId);
            fileQueryWrapper.eq(File::getParentId, file.getId());
            fileQueryWrapper.eq(File::getStatus, IN_TRASH_STATUS);
            fileQueryWrapper.eq(File::getIsDelete, NOT_DELETE);
            List<File> fileList = this.list(fileQueryWrapper);
            if (!fileList.isEmpty()) {
                restoreChild(fileList, userId);
            }
        }
    }

    public void isRepetition(File restoreFile, File file, Integer userId) {
        if (file.getFilename() != null) {
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
        queryWrapper.ne(File::getId, restoreFile.getId());
        Integer parentId = restoreFile.getParentId();
        if (parentId == null) {
            queryWrapper.isNull(File::getParentId);
        } else {
            queryWrapper.eq(File::getParentId, restoreFile.getParentId());
        }
        queryWrapper.eq(File::getIsDelete, NOT_DELETE);
        File file1 = this.getOne(queryWrapper);
        if (null != file1) {
            isRepetition(restoreFile, file1, userId);
        }
    }

    public void deleteChild(File file, Integer userId) {
        if (file.getFiletype().equals(DIR_FILE_TYPE)) {
            LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(File::getUserId, userId);
            queryWrapper.eq(File::getIsDelete, NOT_DELETE);
            queryWrapper.eq(File::getParentId, file.getId());
            List<File> files = this.list(queryWrapper);
            if (IN_TRASH_STATUS.equals(file.getStatus())) {
                file.setIsDelete(DELETED);
            } else {
                file.setStatus(IN_TRASH_STATUS);
            }
            this.updateById(file);
            for (File file1 : files) {
                deleteChild(file1, userId);
            }
        } else {
            if (IN_TRASH_STATUS.equals(file.getStatus())) {
                file.setIsDelete(DELETED);
            } else {
                file.setStatus(IN_TRASH_STATUS);
            }
            this.updateById(file);
        }
    }

    private boolean renameFile(List<Integer> operationList, String rename, FileVo fileVo, LambdaQueryWrapper<File> dirQueryWrapper) {
        dirQueryWrapper.eq(File::getId, operationList.get(0));
        File file = this.getOne(dirQueryWrapper);
        if (file != null) {
            file.setFilename(rename);
            this.updateById(file);
            fileVo.setCode(0);
            fileVo.setMsg(RENAME_SUCCESS);
            return true;
        }
        return false;
    }

    private Integer isExistFile(String[] fullPath, String originalFilename, Integer userId, Integer parentId) throws Exception {
        if (fullPath.length > 1) {
            parentId = createParent(userId, parentId, fullPath);
        } else {
            String[] split = originalFilename.split("/");
            if (split.length > 1) {
                parentId = createParent(userId, parentId, split);
            }
        }
        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        if (parentId == 0) {
            queryWrapper.isNull(File::getParentId);
        } else {
            queryWrapper.eq(File::getParentId, parentId);
        }
        queryWrapper.eq(File::getUserId, userId);
        queryWrapper.eq(File::getFilename, originalFilename);
        queryWrapper.ne(File::getFiletype, DIR_FILE_TYPE);
        queryWrapper.eq(File::getIsDelete, NOT_DELETE);
        File one = this.getOne(queryWrapper);
        if (one != null) {
            throw new FileUploadException(UPLOAD_FILE_ERROR);
        }
        return parentId;
    }

    private void moveFiles(Integer userId, List<Integer> operationList, Integer parentId) throws Exception {
        File parentObj;
        List<File> parentObjList;
        List<File> objList;
        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<File> fileLambdaQueryWrapper = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<File> objLambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (parentId == 0) {
            queryWrapper.isNotNull(File::getId);
            fileLambdaQueryWrapper.isNull(File::getId);
            objLambdaQueryWrapper.isNull(File::getParentId);
        } else {
            queryWrapper.ne(File::getId, parentId);
            fileLambdaQueryWrapper.eq(File::getId, parentId);
            objLambdaQueryWrapper.eq(File::getParentId, parentId);
        }
        fileLambdaQueryWrapper.eq(File::getUserId, userId);
        fileLambdaQueryWrapper.eq(File::getIsDelete, NOT_DELETE);
        objLambdaQueryWrapper.eq(File::getUserId, userId);
        objLambdaQueryWrapper.eq(File::getIsDelete, NOT_DELETE);
        objList = this.list(objLambdaQueryWrapper);
        parentObjList = this.list(fileLambdaQueryWrapper);
        queryWrapper.eq(File::getUserId, userId);
        queryWrapper.eq(File::getIsDelete, NOT_DELETE);
        queryWrapper.in(File::getId, operationList);
        List<File> files = this.list(queryWrapper);
        if (!files.isEmpty()) {
            for (File file : files) {
                for (File file1 : objList) {
                    if (file1.getFilename().equals(file.getFilename()) && file1.getFiletype().equals(file.getFiletype())) {
                        throw new MoveException(MOVE_FILE_ERROR);
                    }
                }
                if (file.getParentId() == null && !parentObjList.isEmpty()) {
                    parentObj = parentObjList.get(0);
                    while (parentObj.getParentId() != null) {
                        if (parentObj.getParentId().equals(file.getId())) {
                            throw new MoveException(MOVE_TO_SELF_ERROR);
                        }
                        parentObj = this.getById(parentObj.getParentId());
                    }
                }
                while (file.getParentId() != null) {
                    if (file.getParentId().equals(parentId)) {
                        throw new MoveException(MOVE_TO_SELF_ERROR);
                    }
                    file = this.getById(file.getParentId());
                }
            }
            for (File file : files) {
                if (parentId != 0) {
                    file.setParentId(parentId);
                    this.updateById(file);
                } else {
                    LambdaUpdateWrapper<File> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.set(File::getParentId, null);
                    updateWrapper.in(File::getId, operationList);
                    this.update(file, updateWrapper);
                }
            }
        } else {
            throw new MoveException(MOVE_TO_SELF_ERROR);
        }
    }


    private void getFiles(Integer userId, List<File> fileList, List<File> list) {
        LambdaQueryWrapper<File> fileLambdaQueryWrapper;
        for (File file : list) {
            if (!DIR_FILE_TYPE.equals(file.getFiletype())) {
                fileList.add(file);
            } else {
                fileLambdaQueryWrapper = new LambdaQueryWrapper<>();
                fileLambdaQueryWrapper.eq(File::getUserId, userId);
                fileLambdaQueryWrapper.eq(File::getStatus, NOT_IN_TRASH_STATUS);
                fileLambdaQueryWrapper.eq(File::getParentId, file.getId());
                List<File> files = this.list(fileLambdaQueryWrapper);
                if (!list.isEmpty()) {
                    getFiles(userId, fileList, files);
                }
            }
        }
    }

    private void getBread(Integer fileId, List<List<Object>> bread) {
        File file = this.getById(fileId);
        if (null != file) {
            ArrayList<Object> list = new ArrayList<>();
            list.add(file.getId());
            list.add(file.getFilename());
            bread.add(0, list);
        }
        while (file != null && file.getParentId() != null) {
            ArrayList<Object> list = new ArrayList<>();
            file = this.getById(file.getParentId());
            list.add(file.getId());
            list.add(file.getFilename());
            bread.add(0, list);
        }
    }
}
