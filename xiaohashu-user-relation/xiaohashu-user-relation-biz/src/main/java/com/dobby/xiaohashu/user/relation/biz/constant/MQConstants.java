package com.dobby.xiaohashu.user.relation.biz.constant;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/4/17 10:30
 * 声明发送MQ需要用到的Topic主题、Tag标签
 */
public interface MQConstants {

    /**
     * Topic: 关注、取关共用一个
     */
    String TOPIC_FOLLOW_OR_UNFOLLOW = "FollowUnfollowTopic";

    /**
     * 关注标签
     */
    String TAG_FOLLOW = "Follow";

    /**
     * 取关标签
     */
    String TAG_UNFOLLOW = "Unfollow";

    /**
     * Topic: 关注数计数
     */
    String TOPIC_COUNT_FOLLOWING = "CountFollowingTopic";

    /**
     * Topic: 粉丝数计数
     */
    String TOPIC_COUNT_FANS = "CountFansTopic";
}
