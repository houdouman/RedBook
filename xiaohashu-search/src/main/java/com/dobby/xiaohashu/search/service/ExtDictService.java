package com.dobby.xiaohashu.search.service;

import org.springframework.http.ResponseEntity;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/26 18:23
 */
public interface ExtDictService {

    /**
     * 获取热更新词典
     * @return
     */
    ResponseEntity<String> getHotUpdateExtDict();
}
