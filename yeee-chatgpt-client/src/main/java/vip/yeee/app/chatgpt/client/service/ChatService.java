package vip.yeee.app.chatgpt.client.service;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import okhttp3.sse.EventSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vip.yeee.app.chatgpt.client.constant.ChatGptConstant;
import vip.yeee.app.chatgpt.client.domain.local.ChatLocalRepository;
import vip.yeee.app.chatgpt.client.domain.redis.ChatRedisRepository;
import vip.yeee.app.chatgpt.client.kit.ChatAppNoticeKit;
import vip.yeee.app.chatgpt.client.listener.ChatAppWsContext;
import vip.yeee.app.chatgpt.client.model.ChatMessage2;
import vip.yeee.app.chatgpt.client.model.ChatParams;
import vip.yeee.app.chatgpt.client.listener.WsEventSourceListener;
import vip.yeee.app.chatgpt.client.properties.OpenaiApiProperties;
import vip.yeee.app.common.kit.CheckRepeatKit;

import javax.annotation.Resource;
import java.util.*;

/**
 * OpenAI相关服务实现
 */
@Slf4j
@Service
public class ChatService {

    @Resource
    private OpenaiApiProperties openaiApiProperties;
    @Autowired
    ChatAuthService authService;
    @Resource
    private OpenaiApiService openaiApiService;
    @Resource
    private ChatRedisRepository chatRedisRepository;
    @Resource
    private CheckRepeatKit checkRepeatKit;

    // 构建请求头
    public Map<String, String> headers() {
        Map<String,String> headers = new HashMap<>();
        String apiToken;
        if (StrUtil.isNotBlank(chatRedisRepository.getApiToken())) {
            apiToken = chatRedisRepository.getApiToken();
        } else {
            apiToken = openaiApiProperties.getChat().getKey();
        }
        headers.put("Authorization","Bearer " + RandomUtil.randomEle(StrUtil.split(apiToken, ",")));
        return headers;
    }

    // 构建用户消息
    public ChatMessage2 buildUserMessage(String content) {
        ChatMessage2 message = new ChatMessage2();
        message.setRole(ChatGptConstant.ChatRole.USER);
        message.setContent(content);
        return message;
    }

    public void doWsChat(String msg, String chatId, String uid) {
        WsEventSourceListener listener = new WsEventSourceListener(chatId, uid, msg);

        if (this.handleAdminRequest(msg, listener)) {
            return;
        }

        Integer count = chatRedisRepository.getULimitCount();
        String limitUserKey = StrUtil.isNotBlank(chatRedisRepository.getUserOpenIdCache(uid)) ? chatRedisRepository.getUserOpenIdCache(uid) : uid;
        if (count != null && (StrUtil.isBlank(chatRedisRepository.getULimitExclude())
                || Arrays.stream(chatRedisRepository.getULimitExclude().split(",")).noneMatch(ex -> ex.equals(uid) || ex.equals(limitUserKey)))
                && Optional.ofNullable(chatRedisRepository.getULimitCountCache(limitUserKey)).orElse(0) >= count) {
            ChatAppNoticeKit.sendUseLimitMsg(listener, count);
            return;
        }
        String temp = msg.toLowerCase().replaceAll(chatRedisRepository.getReplaceRegex(), "");
        String presetMsg = chatRedisRepository.getPresetAnswers().get(temp);
        if (StrUtil.isNotBlank(presetMsg)) {
            ChatAppNoticeKit.sendPresetMsg(listener, presetMsg);
            return;
        }
        if (chatRedisRepository.getProhibitKeyword().stream().anyMatch(key -> StrUtil.contains(temp, key))) {
            ChatAppNoticeKit.sendPresetMsg(listener, Lists.newArrayList(chatRedisRepository.getPresetAnswers().values()).get(0));
            return;
        }
        if (ChatLocalRepository.containSensWord(temp)) {
            ChatAppNoticeKit.sendPresetMsg(listener, Lists.newArrayList(chatRedisRepository.getPresetAnswers().values()).get(0));
            return;
        }

        boolean canDo = checkRepeatKit.canRepeatDoSendMsg(uid, 8);
        if (!canDo) {
            ChatAppNoticeKit.sendQuesFastMsg(listener);
            return;
        }
        listener.onMsg(false, "load", ChatGptConstant.MsgTemplate.FIRST_ON_MESSAGE);
        listener.setOnComplete(lt -> {
            log.info("【CHAT-GPT】- 收到消息 = {}", lt.getLastMessage());
        });
        listener.setFirstReceiveFunc(lt -> {
            log.info("【CHAT-GPT】- 首次响应");
            chatRedisRepository.incrULimitCountCache(limitUserKey);
        });
        listener.setCloseFunc(lt -> {
            if (!lt.isHasDone()) {
                listener.onMsg(false, "chat", ChatGptConstant.MsgTemplate.CONTINUE_MESSAGE);
            }
        });
        ChatParams params = new ChatParams();
        List<ChatMessage2> messages = ChatLocalRepository.getChatContext(chatId, chatRedisRepository.getPreRecordCount());
        ChatMessage2 chatMessage2 = this.buildUserMessage(msg);
//        chatMessage2.setMagId(listener.);
        messages.add(chatMessage2);
        params.setMessages(messages);
        params.setUser(authService.getUserId());
        params.setStream(true);
        listener.setStartTime(new Date());
        EventSource eventSource = openaiApiService.chatCompletions(headers(), params, listener);
        listener.setEventSource(eventSource);
        ChatAppWsContext.setUserRecentESL(chatId, uid, listener);
    }

    private boolean handleAdminRequest(String msg, WsEventSourceListener listener) {
        if (msg.startsWith("yeee-grant")) {
            for (String key : StrUtil.split(chatRedisRepository.getApiToken(), ",")) {
                String response;
                try {
                    Map<String,String> headers = ImmutableMap.of("Authorization","Bearer " + key, "Content-Type", "application/json");
                    response = openaiApiService.queryBillingCreditGrants(headers);
                    listener.onMsg(false, "chat", "总量：" + JSON.parseObject(response).getString("hard_limit_usd"));
                    listener.onMsg(false, "chat", "\n");
                    listener.onMsg(false, "chat", "到期：" + DateUtil.date(JSON.parseObject(response).getLong("access_until") * 1000));
                    listener.onMsg(false, "chat", "\n");
                    Date now = new Date();
                    response = openaiApiService.queryBillingUsed(DateUtil.format(DateUtil.offset(now, DateField.DAY_OF_MONTH, -99), DatePattern.NORM_DATE_PATTERN)
                            , DateUtil.format(DateUtil.offset(now, DateField.DAY_OF_MONTH, 1), DatePattern.NORM_DATE_PATTERN), headers);
                    listener.onMsg(false, "chat", "已使用：" + JSON.parseObject(response).getBigDecimal("total_usage").doubleValue() / 100);
                    listener.onMsg(false, "chat", "\n");
                    listener.onMsg(false, "chat", "-----------------------------");
                    listener.onMsg(false, "chat", "\n");
                } catch (Exception e) {
                    log.error("【CHAT-GPT】- 查询apiKey剩余额度失败，key = {}", key);
                }
            }
            return true;
        }
        return false;
    }
}
