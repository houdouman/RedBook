package com.dobby.xiaohashu.oss.biz.service.impl;

import com.dobby.framework.biz.context.interceptor.FeignRequestInterceptor;
import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.oss.biz.service.FileService;
import com.dobby.xiaohashu.oss.biz.strategy.FileStrategy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/2 17:31
 */
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Resource
    private FileStrategy fileStrategy;


    @Override
    public Response<?> uploadFile(MultipartFile file) {
        //上传文件
        String url = fileStrategy.uploadFile(file, "xiaohashu-dobby");
        return Response.success(url);
    }
}
