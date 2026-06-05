package com.dobby.xiaohashu.note.biz.rpc;

import com.dobby.xiaohashu.distributed.id.generator.api.DistributedIdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/11 18:51
 */
@Component
public class DistributedIdGeneratorRpcService {
    @Resource
    private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

    /**
     * 生成雪花算法 ID
     *
     * @return
     */
    public String getSnowflakeId() {
        return distributedIdGeneratorFeignApi.getSnowflakeId("test");
    }
}
