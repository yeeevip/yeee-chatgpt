package vip.yeee.app.chatgpt.client.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vip.yeee.app.chatgpt.client.biz.ApiChatGptBiz;
import vip.yeee.app.chatgpt.client.model.vo.UserAuthVo;
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
        return CommonResult.success(apiChatGptBiz.wsAuth(jscode));
    }

}
