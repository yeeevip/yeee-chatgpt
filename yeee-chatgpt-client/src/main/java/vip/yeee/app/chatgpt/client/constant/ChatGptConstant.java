package vip.yeee.app.chatgpt.client.constant;

/**
 * description......
 *
 * @author yeeee
 * @since 2023/4/9 11:55
 */
public class ChatGptConstant {

    public interface MsgTemplate {

        String FIRST_ON_MESSAGE = "WeSo正在拼命加载中，根据提问的复杂度将会有5-20秒的等待时间哦^_~\n\n"
                + "tip1：退出后下次进来会继续回答哦。\n\n"
                + "tip2：剩余次数[每天凌晨]会重置哦。\n";

        String CONTINUE_MESSAGE = "\n\n\n\n回复 【继续】 WeSo会继续回答哦~~~~";
    }

    public interface ChatRole {

        // 系统角色
        String SYSTEM = "system";
        // 用户角色
        String USER = "user";
        // 助理角色
        String ASSISTANT = "assistant";
    }

    public interface ChatUserID {

        String U_ID = "uid";

        String CHAT_ID = "chatId";
    }

}
