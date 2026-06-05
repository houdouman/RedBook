package com.dobby.xiaohashu.auth.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.dobby.framework.biz.context.filter.LoginUserContextHolder;
import com.dobby.framework.common.exception.BizException;
import com.dobby.framework.common.response.Response;
import com.dobby.xiaohashu.auth.constant.RedisKeyConstants;
import com.dobby.xiaohashu.auth.enums.LoginTypeEnum;
import com.dobby.xiaohashu.auth.enums.ResponseCodeEnum;
import com.dobby.xiaohashu.auth.model.vo.user.UserLoginReqVO;
import com.dobby.xiaohashu.auth.rpc.UserRpcService;
import com.dobby.xiaohashu.auth.service.AuthService;
import com.dobby.xiaohashu.user.dto.rsp.FindUserByPhoneRspDTO;
import com.google.common.base.Preconditions;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 15:06
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private UserRpcService userRpcService;


    /**
     * 用户登录和注册
     * @param userLoginReqVO
     * @return
     */
    @Override
    public Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO) {
        String phone = userLoginReqVO.getPhone();
        Integer type = userLoginReqVO.getType();

        LoginTypeEnum loginTypeEnum = LoginTypeEnum.valueOf(type);
        Long userId = null;

        //判断登录类型
        switch (loginTypeEnum) {
            case VERIFICATION_CODE: //验证码登录
                String code = userLoginReqVO.getCode();
                // 校验入参验证码是否为空
                Preconditions.checkArgument(StringUtils.isNotBlank(code), "验证码不能为空");
                String key = RedisKeyConstants.buildVerificationCodeKey(phone);
                String sentCode = (String)redisTemplate.opsForValue().get(key);
                //如果用户提交的验证码与redis中存储的验证码不一致
                if(!StringUtils.equals(code, sentCode)){
                    throw new BizException(ResponseCodeEnum.VERIFICATION_CODE_ERROR);
                }
                //判断用户是否注册
                Long userIdTmp1 = userRpcService.registerUser(phone);
                if(Objects.isNull(userIdTmp1)){
                    throw new BizException(ResponseCodeEnum.LOGIN_FAIL);
                }
                userId = userIdTmp1;
                break;
            case PASSWORD:
                String password = userLoginReqVO.getPassword();
                if(StringUtils.isBlank(password)){
                    return Response.fail(ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode(), "密码不能为空");
                }
                //判断用户是否注册
                Long userIdTmp2 = userRpcService.registerUser(phone);
                if(Objects.isNull(userIdTmp2)){
                    throw new BizException(ResponseCodeEnum.LOGIN_FAIL);
                }
                //通过手机号查询用户
                FindUserByPhoneRspDTO findUserByPhoneRspDTO = userRpcService.findUserByPhone(phone);
                //用户是否注册
                if(Objects.isNull(findUserByPhoneRspDTO)){
                    throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
                }
                String storedPassword = findUserByPhoneRspDTO.getPassword();
                boolean matches = passwordEncoder.matches(password, storedPassword);
                if(!matches){
                    throw new BizException(ResponseCodeEnum.PHONE_OR_PASSWORD_ERROR);
                }
                userId = findUserByPhoneRspDTO.getId();
                break;
            default:
                break;
        }
        //登录用户
        StpUtil.login(userId);
        //获取token
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        return Response.success(tokenInfo.tokenValue);
    }

    /**
     * 退出登录
     * @return
     */
    @Override
    public Response<?> logout() {
        Long userId = LoginUserContextHolder.getUserId();

        // 退出登录 (指定用户 ID)，其已经实现删除redis中token
        StpUtil.logout(userId);

        return Response.success("成功退出登录");
    }

}
