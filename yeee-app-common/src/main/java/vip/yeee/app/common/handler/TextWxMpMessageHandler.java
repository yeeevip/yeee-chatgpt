package vip.yeee.app.common.handler;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * description ...
 * @author yeeeeee
 * @since 2022/2/22 11:19
 */
@Slf4j
@Component
public class TextWxMpMessageHandler implements WxMpMessageHandler {

    private final static String WESO_APP_URL = "<a href=\"{}\">WeSo小助手</a>" +
            "\n\n" +
            "温馨提示：进入页面后即可体验，也可以复制链接到PC或者本地浏览器打开哦！";

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage, Map<String, Object> context, WxMpService wxMpService, WxSessionManager sessionManager) {
        log.info("WxMpMessageHandler - 处理文本消息 message = {}", wxMessage.getContent());
        return null;
    }
}
