package vip.yeee.app.chatgpt.client.biz;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.yeee.app.chatgpt.client.constant.ChatGptConstant;
import vip.yeee.app.chatgpt.client.domain.redis.ChatRedisRepository;
import vip.yeee.app.chatgpt.client.listener.ChatAppWsContext;
import vip.yeee.app.chatgpt.client.listener.WsEventSourceListener;
import vip.yeee.app.chatgpt.client.service.ChatService;
import vip.yeee.app.common.service.CommonService;
import vip.yeee.memo.base.model.rest.CommonResult;
import vip.yeee.memo.base.web.utils.HttpRequestUtils;
import vip.yeee.memo.base.web.utils.SpringContextUtils;

import javax.annotation.Resource;
import vip.yeee.memo.common.websocket.netty.bootstrap.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * description......
 *
 * @author yeeee
 * @since 2023/6/21 9:46
 */
@Slf4j
@Component
public class ApiChatGptBiz {

    @Resource
    private ChatRedisRepository chatRedisRepository;
    @Resource
    private WxMaService wxMaService;
    @Resource
    private ChatService chatService;

    public Object adOnClose(String token) {
        String uid = chatRedisRepository.checkToken(token);
        if (StrUtil.isBlank(uid)) {
            return CommonResult.failed("ierror");
        }
        Integer incr = 3;
        String limitUserKey = StrUtil.isNotBlank(chatRedisRepository.getUserOpenIdCache(uid)) ? chatRedisRepository.getUserOpenIdCache(uid) : uid;
        chatRedisRepository.incrULimitCountCache(limitUserKey, incr * (-1L));
        HashMap<String, Object> res = Maps.newHashMap();
        res.put("incr", incr);
        return res;
    }

    public Object chatSurplus(String token) {
        String uid = chatRedisRepository.checkToken(token);
        if (StrUtil.isBlank(uid)) {
            return CommonResult.failed("ierror");
        }
        Map<String, Object> res = Maps.newHashMap();
        String uKey = StrUtil.isNotBlank(chatRedisRepository.getUserOpenIdCache(uid)) ? chatRedisRepository.getUserOpenIdCache(uid) : uid;
        res.put("limitCount", chatRedisRepository.getUserSurplus(uid, uKey));
        return res;
    }

    public Object wsAuth(String jscode) throws Exception {
        String ipAddr = HttpRequestUtils.getIpAddr(SpringContextUtils.getHttpServletRequest());
        String token = chatRedisRepository.genTokenAndCache(ipAddr);
        Map<String, Object> res = Maps.newHashMap();
        res.put("token", token);
        if (StrUtil.isNotBlank(jscode)) {
            WxMaJscode2SessionResult sessionInfo = wxMaService.switchoverTo("wx0d6dadb626269833").getUserService().getSessionInfo(jscode);
            chatRedisRepository.saveUserOpenIdCache(ipAddr, sessionInfo.getOpenid());
            res.put("openid", sessionInfo.getOpenid());
        }
        String uKey = res.containsKey("openid") ? res.get("openid").toString() : ipAddr;
        res.put("limitCount", chatRedisRepository.getUserSurplus(ipAddr, uKey));
        return res;
    }

    public void handleWsOnOpen(Session session, String chatId, String token) throws IOException {
        ChatRedisRepository redisCache = (ChatRedisRepository)SpringContextUtils.getBean(ChatRedisRepository.class);
        String uid = redisCache.checkToken(token);
        if (StrUtil.isBlank(uid)) {
            log.warn("【ws - 身份验证失败】- chatId = {}, token = {}", chatId, token);
            session.close();
        }

        String limitUserKey = StrUtil.isNotBlank(redisCache.getUserOpenIdCache(uid)) ? redisCache.getUserOpenIdCache(uid) : uid;
        redisCache.recordUserDetail(limitUserKey, "ip", uid);
        CommonService commonService = (CommonService) SpringContextUtils.getBean(CommonService.class);
        redisCache.recordUserDetail(limitUserKey, "address", commonService.getCachedAddressByIp(uid));

        Session lastSession = ChatAppWsContext.getUserSession(chatId, uid);
        if (lastSession != null && lastSession.isOpen()) {
            lastSession.close();
        }
        ChatAppWsContext.setUserSession(chatId, uid, session);
        WsEventSourceListener sourceListener = ChatAppWsContext.getUserRecentESL(chatId, uid);
        if (sourceListener != null) {
            sourceListener.startThreadSendWsMsg();
        }
        log.info("[UID：{}，CHAT_ID：{}] 建立连接, 当前连接数:{}", uid, chatId, ChatAppWsContext.allUserSession().size());
    }

    public void handleWsOnMessage(Session session, String msg) {
        String chatId = session.getAttribute(ChatGptConstant.ChatUserID.CHAT_ID);
        String uid = session.getAttribute(ChatGptConstant.ChatUserID.U_ID);
        log.info("[连接ID:{}] 收到消息:{}", uid, msg);
        chatService.doWsChat(msg, chatId, uid);
    }

    public void handleWsOnClose(Session session) {
        String chatId = session.getAttribute(ChatGptConstant.ChatUserID.CHAT_ID);
        String uid = session.getAttribute(ChatGptConstant.ChatUserID.U_ID);
        WsEventSourceListener sourceListener = ChatAppWsContext.getUserRecentESL(chatId, uid);
        if (sourceListener != null) {
            sourceListener.setCanReply(false);
        }
        ChatAppWsContext.removeUserSession(chatId, uid);
        ChatRedisRepository redisCache = (ChatRedisRepository)SpringContextUtils.getBean(ChatRedisRepository.class);
        String uKey = StrUtil.isNotBlank(redisCache.getUserOpenIdCache(uid)) ? redisCache.getUserOpenIdCache(uid) : uid;
        Integer userSurplus = redisCache.getUserSurplus(uid, uKey);
        int onlineCount = ChatAppWsContext.allUserSession().size();
        String logStr = "[连接ID:{}{}{}] 断开连接, 剩余次数：{}，当前连接数：{}";
        log.info(logStr, uid, "", userSurplus, onlineCount);
    }

    public void handleWsOnError(Session session, Throwable error) {
//        String chatId = (String) userProperties.get(ChatGptConstant.ChatUserID.CHAT_ID);
        String uid = session.getAttribute(ChatGptConstant.ChatUserID.U_ID);
//        Session userSession = ChatAppWsContext.getUserSession(chatId, uid);
        log.error("[连接ID:{}] onError ", uid, error);
    }
}
