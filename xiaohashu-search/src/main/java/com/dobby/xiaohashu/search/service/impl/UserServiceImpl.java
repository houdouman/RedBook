package com.dobby.xiaohashu.search.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.dobby.framework.common.response.PageResponse;
import com.dobby.framework.common.util.NumberUtils;
import com.dobby.xiaohashu.search.index.UserIndex;
import com.dobby.xiaohashu.search.model.vo.SearchUserReqVO;
import com.dobby.xiaohashu.search.model.vo.SearchUserRspVO;
import com.dobby.xiaohashu.search.service.UserService;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/26 16:39
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private RestHighLevelClient restHighLevelClient;


    /**
     * 搜索用户
     * @param searchUserReqVO
     * @return
     */
    @Override
    public PageResponse<SearchUserRspVO> searchUser(SearchUserReqVO searchUserReqVO) {
        //查询关键词
        String keyword = searchUserReqVO.getKeyword();
        //当前页码
        Integer pageNo = searchUserReqVO.getPageNo();
        //构建SearchRequest，指定索引
        SearchRequest searchRequest = new SearchRequest(UserIndex.NAME);
        //构建查询内容
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //构建 multi_match 查询，查询 nickname 和 xiaohashu_id 字段
        sourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, UserIndex.FIELD_USER_NICKNAME, UserIndex.FIELD_USER_XIAOHASHU_ID));
        //排序，按fans_total降序
        SortBuilder<?> sortBuilder = new FieldSortBuilder(UserIndex.FIELD_USER_FANS_TOTAL).order(SortOrder.DESC);
        sourceBuilder.sort(sortBuilder);

        //设置分页
        int pageSize = 10;
        int from = (pageNo - 1) * pageSize;
        sourceBuilder.from(from);
        sourceBuilder.size(pageSize);

        //设置高亮字段
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field(UserIndex.FIELD_USER_NICKNAME)
                .preTags("<strong>")
                        .postTags("</strong>");
        sourceBuilder.highlighter(highlightBuilder);

        //将构建的查询条件设置到 SearchRequest 中
        searchRequest.source(sourceBuilder);
        List<SearchUserRspVO> searchUserRspVOS = null;
        long total = 0;

        try {
            log.info("==> SearchRequest: {}", searchRequest);
            //执行查询请求
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            total = searchResponse.getHits().getTotalHits().value;
            log.info("==> 命中文档总数, hits: {}", total);
            searchUserRspVOS = Lists.newArrayList();
            //获取搜索命中的文档列表
            SearchHits searchHits = searchResponse.getHits();
            for(SearchHit hit : searchHits) {
                //获取文档的所有字段
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                //提取特定字段
                Long userId = ((Number) sourceAsMap.get(UserIndex.FIELD_USER_ID)).longValue();
                String nickname = ((String) sourceAsMap.get(UserIndex.FIELD_USER_NICKNAME));
                String avatar = (String) sourceAsMap.get(UserIndex.FIELD_USER_AVATAR);
                String xiaohashuId = (String) sourceAsMap.get(UserIndex.FIELD_USER_XIAOHASHU_ID);
                Integer noteTotal = (Integer) sourceAsMap.get(UserIndex.FIELD_USER_NOTE_TOTAL);
                Integer fansTotal = (Integer) sourceAsMap.get(UserIndex.FIELD_USER_FANS_TOTAL);
                //获取高亮字段
                String highlightedNickname = null;
                if(CollUtil.isNotEmpty(hit.getHighlightFields())
                        && hit.getHighlightFields().containsKey(UserIndex.FIELD_USER_NICKNAME)){
                    highlightedNickname = hit.getHighlightFields().get(UserIndex.FIELD_USER_NICKNAME).fragments()[0].string();
                }

                // 构建 VO 实体类
                SearchUserRspVO searchUserRspVO = SearchUserRspVO.builder()
                        .userId(userId)
                        .nickname(nickname)
                        .avatar(avatar)
                        .xiaohashuId(xiaohashuId)
                        .noteTotal(noteTotal)
                        .fansTotal(NumberUtils.formatNumberString(fansTotal))
                        .highlightNickname(highlightedNickname)
                        .build();
                searchUserRspVOS.add(searchUserRspVO);
            }
        } catch (Exception e) {
            log.error("==> 查询 Elasticserach 异常: ", e);
        }

        return PageResponse.success(searchUserRspVOS, pageNo, total);
    }
}

