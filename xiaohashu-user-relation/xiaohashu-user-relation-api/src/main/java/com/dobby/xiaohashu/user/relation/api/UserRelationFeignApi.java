package com.dobby.xiaohashu.user.relation.api;


import com.dobby.xiaohashu.user.relation.constant.ApiConstants;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/7 21:02
 */
@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface UserRelationFeignApi {
    String PREFIX = "/user/relation";



}
