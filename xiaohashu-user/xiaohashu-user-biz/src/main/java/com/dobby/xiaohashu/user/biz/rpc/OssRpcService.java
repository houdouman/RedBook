package com.dobby.xiaohashu.user.biz.rpc;

import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.oss.api.FileFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/3 16:23
 * 对象存储服务调用
 */
@Component
public class OssRpcService {

    @Resource
    private FileFeignApi fileFeignApi;

    public String uploadFile(MultipartFile file) {
        // 调用对象存储服务上传文件
        Response<?> response = fileFeignApi.uploadFile(file);

        if (!response.isSuccess()) {
            return null;
        }

        // 返回图片访问链接
        return (String) response.getData();
    }
}
