package vip.yeee.app.chatgpt.client.biz;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import io.jsonwebtoken.Claims;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import vip.yeee.app.chatgpt.client.constant.ChatGptConstant;
import vip.yeee.app.chatgpt.client.domain.redis.ChatRedisRepository;
import vip.yeee.app.chatgpt.client.kit.ChatAppNoticeKit;
import vip.yeee.app.chatgpt.client.listener.ChatAppWsContext;
import vip.yeee.app.chatgpt.client.listener.WsEventSourceListener;
import vip.yeee.app.chatgpt.client.model.vo.UserAuthVo;
import vip.yeee.app.chatgpt.client.service.ChatService;
import vip.yeee.app.common.service.CommonService;
import vip.yeee.memo.base.model.exception.BizException;
import vip.yeee.memo.base.web.utils.HttpRequestUtils;
import vip.yeee.memo.base.web.utils.SpringContextUtils;

import javax.annotation.Resource;

import vip.yeee.memo.common.appauth.client.constant.ApiAuthConstant;
import vip.yeee.memo.common.appauth.client.context.ApiSecurityContext;
import vip.yeee.memo.common.appauth.client.kit.JwtClientKit;
import vip.yeee.memo.common.appauth.client.model.ApiAuthedUser;
import vip.yeee.memo.common.appauth.server.kit.JwtServerKit;
import vip.yeee.memo.common.appauth.server.model.vo.JTokenVo;
import vip.yeee.memo.common.websocket.netty.bootstrap.Session;
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
    @Resource
    private JwtServerKit jwtServerKit;
    @Resource
    private JwtClientKit jwtClientKit;

    public Object adOnClose() {
        ApiAuthedUser curUser = ApiSecurityContext.getCurUser();
        String uid = curUser.getUid();
        Integer incr = 3;
        String limitUserKey = StrUtil.isNotBlank(chatRedisRepository.getUserOpenIdCache(uid)) ? chatRedisRepository.getUserOpenIdCache(uid) : uid;
        chatRedisRepository.incrULimitCountCache(limitUserKey, incr * (-1L));
        HashMap<String, Object> res = Maps.newHashMap();
        res.put("incr", incr);
        return res;
    }

    public Object chatSurplus() {
        ApiAuthedUser curUser = ApiSecurityContext.getCurUser();
        String uid = curUser.getUid();
        Map<String, Object> res = Maps.newHashMap();
        String uKey = StrUtil.isNotBlank(chatRedisRepository.getUserOpenIdCache(uid)) ? chatRedisRepository.getUserOpenIdCache(uid) : uid;
        res.put("limitCount", chatRedisRepository.getUserSurplus(uid, uKey));
        return res;
    }

    public UserAuthVo wsAuth(String jscode) throws Exception {
        ApiAuthedUser userVo = new ApiAuthedUser();
        String ipAddr = HttpRequestUtils.getIpAddr(SpringContextUtils.getHttpServletRequest());
        String openid = null;
        if (StrUtil.isNotBlank(jscode)) {
            WxMaJscode2SessionResult sessionInfo = wxMaService.switchoverTo("wx0d6dadb626269833").getUserService().getSessionInfo(jscode);
            openid = sessionInfo.getOpenid();
            chatRedisRepository.saveUserOpenIdCache(ipAddr, openid);
        }
        userVo.setUid(ipAddr);
        userVo.setOpenid(openid);
        JTokenVo jTokenVo = jwtServerKit.createToken(JSON.toJSONString(userVo));
        UserAuthVo authVo = new UserAuthVo();
        authVo.setAccessToken(jTokenVo.getAccessToken());
        authVo.setOpenid(openid);
        String uKey = StrUtil.isNotBlank(openid) ? openid : ipAddr;
        authVo.setLimitCount(chatRedisRepository.getUserSurplus(ipAddr, uKey));
        return authVo;
    }

    public void handleWsOnOpen(Session session, HttpHeaders headers, String chatId, MultiValueMap<String, String> reqParams) {
        session.setSubprotocols("Utoken");
        ChatRedisRepository redisCache = (ChatRedisRepository)SpringContextUtils.getBean(ChatRedisRepository.class);
        ApiAuthedUser userVo;
        String token = null;
        try {
            token = headers.get(ApiAuthConstant.TOKEN);
            if ((StrUtil.isBlank(token) && StrUtil.isBlank(token = reqParams.getFirst(ApiAuthConstant.TOKEN)))
                    || StrUtil.isBlank(token = token.replace(ApiAuthConstant.JWT_TOKEN_PREFIX, ""))) {
                throw new BizException("token null");
            }
            Claims claims = jwtClientKit.getTokenClaim(token);
            userVo = JSON.parseObject(claims.getSubject(), ApiAuthedUser.class);
        } catch (Exception e) {
            log.warn("【ws - 身份验证失败】- chatId = {}, token = {}", chatId, token);
            ChatAppNoticeKit.sendAuthFailedMsg(session);
            return;
        }

        String uid = userVo.getUid();

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
        if (chatId == null || uid == null) {
            return;
        }
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

    public void handleWsOnEvent(Session session, Object evt) {
        String uid = session.getAttribute(ChatGptConstant.ChatUserID.U_ID);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            switch (idleStateEvent.state()) {
                case READER_IDLE:
                    log.info("[连接ID:{}] read idle ", uid);
                    ChatAppNoticeKit.sendHeartTimeoutMsg(session);
                    break;
                case WRITER_IDLE:
                    log.info("write idle");
                    break;
                case ALL_IDLE:
                    log.info("all idle");
                    break;
                default:
                    break;
            }
        }
    }
}
