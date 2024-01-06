package vip.yeee.app.chatgpt.client.domain.local;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.tokenizer.TokenizerEngine;
import cn.hutool.extra.tokenizer.Word;
import com.google.common.collect.Sets;
import vip.yeee.app.chatgpt.client.model.ChatMessage2;
import vip.yeee.memo.base.web.utils.SpringContextUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 携带信息
 */
public class ChatLocalRepository {

    // 本地缓存的信息 -- 修改 capacity 设置最大容量
    private static final Cache<String, List<ChatMessage2>> chats = CacheUtil.newFIFOCache(100);
    private static final Set<String> SENSITIVE_LEXICON = Sets.newHashSet();

    public static void put(String key, List<ChatMessage2> value) {
        int listSize = value.size();
        List<ChatMessage2> newMessages = value;
        if (listSize > 10){
            newMessages = value.subList(listSize-10,listSize);
        }
        chats.put(key,newMessages);
    }

    public static List<ChatMessage2> get(String key) {
        List<ChatMessage2> messages = chats.get(key);
        if (Validator.isEmpty(messages)){
            return new ArrayList<>();
        }else {
            return messages;
        }
    }

    // 获取上下文
    public static List<ChatMessage2> getChatContext(String chatId) {
        List<ChatMessage2> messages = ChatLocalRepository.get(chatId);
        return messages;
    }

    // 获取上下文 -- 指定条数
    public static List<ChatMessage2> getChatContext(String chatId, Integer num) {
        List<ChatMessage2> messages = getChatContext(chatId);
        int msgSize = messages.size();

        if (msgSize > num){
            return messages.subList(msgSize - num,msgSize);
        }else {
            return messages;
        }
    }

    public static void addSensWord(String word) {
        if (StrUtil.isBlank(word) || word.length() == 1) {
            return;
        }
        SENSITIVE_LEXICON.add(word);
    }

    public static boolean containSensWord(String sentence) {
        if (StrUtil.isBlank(sentence)) {
            return false;
        }
        Iterator<Word> it;
        it = ((TokenizerEngine)SpringContextUtils.getBean(TokenizerEngine.class)).parse(sentence);
        boolean flag = false;
        if(IterUtil.isNotEmpty(it)) {
            while(it.hasNext()) {
                String text = it.next().getText();
                if (StrUtil.isBlank(text) || text.length() == 1) {
                    continue;
                }
                if (SENSITIVE_LEXICON.contains(text.replaceAll(" ", ""))) {
                    flag = true;
                }
            }
        }
        return flag;
    }

}
