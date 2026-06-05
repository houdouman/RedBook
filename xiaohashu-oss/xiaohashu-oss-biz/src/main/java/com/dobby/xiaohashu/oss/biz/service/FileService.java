package com.dobby.xiaohashu.oss.biz.service;

import com.dobby.framework.common.response.Response;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/2 17:30
 */
public interface FileService {


    /**
     * 上传文件
     * @param file
     * @return
     */
    Response<?> uploadFile(MultipartFile file);
}
