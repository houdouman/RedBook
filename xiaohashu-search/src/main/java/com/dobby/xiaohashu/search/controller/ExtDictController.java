package com.dobby.xiaohashu.search.controller;

import com.dobby.framework.biz.operationlog.aspect.ApiOperationLog;
import com.dobby.xiaohashu.search.service.ExtDictService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/26 18:25
 * 热更新拓展词典
 */
@RestController
@RequestMapping("/search")
@Slf4j
public class ExtDictController {

    @Resource
    private ExtDictService extDictService;

    @GetMapping("/ext/dict")
    @ApiOperationLog(description = "热更新词典")
    public ResponseEntity<String> extDict() {
        return extDictService.getHotUpdateExtDict();
    }

}