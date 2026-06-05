package com.dobby.xiaohashu.auth.sms;

import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.dobby.framework.common.util.JsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author 橡皮鸭
 * @version 1.0
 * @date 2026/3/31 11:45
 */
@Component
@Slf4j
public class AliyunSmsHelper {

    @Resource
    private com.aliyun.dypnsapi20170525.Client client;

    /**
     * 发送短信
     * @param signName
     * @param templateCode
     * @param phone
     * @param templateParam
     * @return
     */
    public boolean sendMessage(String signName, String templateCode, String phone, String templateParam) {
        com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest sendSmsVerifyCodeRequest = new com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest()
                .setSignName(signName)
                .setTemplateCode(templateCode)
                .setPhoneNumber(phone)
                .setTemplateParam(templateParam);

        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();

        try {
            log.info("==> 开始短信发送, phone: {}, signName: {}, templateCode: {}, templateParam: {}", phone, signName, templateCode, templateParam);

            // 发送短信
            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCodeWithOptions(sendSmsVerifyCodeRequest, runtime);

            log.info("==> 短信发送成功, response: {}", JsonUtils.toJsonString(response));
            return true;
        } catch (Exception error) {
            log.error("==> 短信发送错误: ", error);
            return false;
        }
    }
}
