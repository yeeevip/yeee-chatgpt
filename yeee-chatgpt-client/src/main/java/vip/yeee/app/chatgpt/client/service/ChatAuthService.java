package vip.yeee.app.chatgpt.client.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatAuthService {

    public String getUserId() {
        return "0";
    }

    public String getUserName() {
        return "匿名用户";
    }
}
