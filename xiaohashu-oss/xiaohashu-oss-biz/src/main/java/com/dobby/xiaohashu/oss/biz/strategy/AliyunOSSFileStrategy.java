package com.dobby.xiaohashu.oss.biz.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/2 17:23
 */
@Slf4j
public class AliyunOSSFileStrategy implements FileStrategy{
    @Override
    public String uploadFile(MultipartFile file, String bucketName) {
        log.info("## 上传文件至阿里云 OSS ...");
        return null;
    }
}
