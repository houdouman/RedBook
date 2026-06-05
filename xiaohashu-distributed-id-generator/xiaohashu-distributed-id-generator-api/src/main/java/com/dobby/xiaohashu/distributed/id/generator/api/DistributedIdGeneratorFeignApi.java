package com.dobby.xiaohashu.distributed.id.generator.api;

import com.dobby.xiaohashu.distributed.id.generator.constant.ApiConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/11 15:59
 */
@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface DistributedIdGeneratorFeignApi {

    String PREFIX = "/id";

    @GetMapping(value = PREFIX + "/segment/get/{key}")
    String getSegmentId(@PathVariable("key") String key);

    @GetMapping(value = PREFIX + "/snowflake/get/{key}")
    String getSnowflakeId(@PathVariable("key") String key);
}
