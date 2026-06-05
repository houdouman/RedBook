package com.dobby.xiaohashu.oss.biz.strategy;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/2 17:20
 * 文件策略接口
 */
public interface FileStrategy {

    /**
     * 上传文件
     * @param file
     * @param bucketName
     * @return
     */
    String uploadFile(MultipartFile file, String bucketName);
}
