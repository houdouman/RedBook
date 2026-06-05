package com.dobby.xiaohashu.oss.api;

import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.oss.config.FeignFormConfig;
import com.dobby.xiaohashu.oss.constant.ApiConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/3 15:35
 */
@FeignClient(name = ApiConstants.SERVICE_NAME, configuration = FeignFormConfig.class) //注册中心注册的服务名称
public interface FileFeignApi {

    String PREFIX = "/file";

    @PostMapping(value = PREFIX + "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Response<?> uploadFile(@RequestPart(value = "file") MultipartFile file);
}
