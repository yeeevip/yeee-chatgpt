package vip.yeee.app.chatgpt.client.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vip.yeee.app.chatgpt.client.biz.ApiChatGptBiz;
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

    @PostMapping("/ad/onClose/{token}")
    public CommonResult<Object> adOnClose(@PathVariable("token") String token) {
        return CommonResult.success(apiChatGptBiz.adOnClose(token));
    }

    @PostMapping("/chat/surplus/{token}")
    public CommonResult<Object> chatSurplus(@PathVariable("token") String token) {
        return CommonResult.success(apiChatGptBiz.chatSurplus(token));
    }

    @GetMapping("/ws-auth")
    public CommonResult<Object> wsAuth(String jscode) throws Exception {
        return CommonResult.success(apiChatGptBiz.wsAuth(jscode));
    }

}
