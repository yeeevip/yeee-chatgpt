package vip.yeee.app.chatgpt.client.listener;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import vip.yeee.app.chatgpt.client.constant.ChatGptConstant;

import vip.yeee.memo.common.websocket.netty.bootstrap.Session;
import java.util.List;
import java.util.Map;

/**
 * description......
 *
 * @author yeeee
 * @since 2023/4/5 16:55
 */
public class ChatAppWsContext {

    private final static Map<String, Session> CUR_USER_SESSION_MAP = Maps.newHashMap();
    private final static Map<String, WsEventSourceListener> CUR_USER_RECENT_ESL_MAP = Maps.newHashMap();

    public static void setUserSession(String chatId, String uid, Session session) {
        session.setAttribute(ChatGptConstant.ChatUserID.CHAT_ID, chatId);
        session.setAttribute(ChatGptConstant.ChatUserID.U_ID, uid);
        CUR_USER_SESSION_MAP.put(chatId + ":" + uid, session);
    }

    public static void removeUserSession(String chatId, String uid) {
        Session userSession = getUserSession(chatId, uid);
        if (userSession != null) {
            userSession.close();
            CUR_USER_SESSION_MAP.remove(chatId + ":" + uid);
        }
    }

    public static Session getUserSession(String chatId, String uid) {
        return CUR_USER_SESSION_MAP.get(chatId + ":" + uid);
    }

    public static List<Session> allUserSession() {
        return Lists.newArrayList(CUR_USER_SESSION_MAP.values());
    }

    public static void setUserRecentESL(String chatId, String uid, WsEventSourceListener listener) {
        CUR_USER_RECENT_ESL_MAP.put(chatId + ":" + uid, listener);
    }

    public static WsEventSourceListener getUserRecentESL(String chatId, String uid) {
        return CUR_USER_RECENT_ESL_MAP.get(chatId + ":" + uid);
    }

}
