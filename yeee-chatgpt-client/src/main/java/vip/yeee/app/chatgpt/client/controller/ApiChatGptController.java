package vip.yeee.app.chatgpt.client.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vip.yeee.app.chatgpt.client.biz.ApiChatGptBiz;
import vip.yeee.app.chatgpt.client.model.vo.ApiAuthedUserVo;
import vip.yeee.app.chatgpt.client.model.vo.UserAuthVo;
import vip.yeee.memo.base.model.exception.BizException;
import vip.yeee.memo.base.model.rest.CommonResult;

import javax.annotation.Resource;

/**
 * description......
 *
 * @author yeeee
 * @since 2023/5/6 10:15
 */
@Slf4j
@RequestMapping("api/airobot")
@RestController
public class ApiChatGptController {

    @Resource
    private ApiChatGptBiz apiChatGptBiz;

//    @AnonymousAccess
//    @PostMapping("/ad/onLoad")
//    public CommonResult<Object> adOnLoad(String jscode) throws Exception {
//        return CommonResult.success(null);
//    }

    @PostMapping("/ad/onClose")
    public CommonResult<Object> adOnClose() {
        return CommonResult.success(apiChatGptBiz.adOnClose());
    }

    @PostMapping("/chat/surplus")
    public CommonResult<Object> chatSurplus() {
        return CommonResult.success(apiChatGptBiz.chatSurplus());
    }

    @PostMapping("/ws-auth")
    public CommonResult<UserAuthVo> wsAuth(String jscode) throws Exception {
        ApiAuthedUserVo userVo = new ApiAuthedUserVo();
        try {
            userVo.setJscode(jscode);
            return CommonResult.success(apiChatGptBiz.wsAuth(userVo));
        } catch (Exception e) {
            if (!(e instanceof BizException)) {
                log.error("wsAuth error", e);
                throw new BizException("身份认证失败！\n\n也可以微信搜索公众号：一页一\n\n回复 weso 重新获取您专属链接继续使用哦！");
            }
            throw e;
        }
    }

}
