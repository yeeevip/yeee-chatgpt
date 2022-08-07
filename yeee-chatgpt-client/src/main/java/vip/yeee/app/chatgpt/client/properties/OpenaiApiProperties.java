package vip.yeee.app.chatgpt.client.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * description......
 *
 * @author yeeee
 * @since 2023/6/21 16:38
 */
@Data
@ConfigurationProperties(prefix = "yeee.openai")
@Configuration
public class OpenaiApiProperties {

    private Chat chat;

    @Data
    public static class Chat {

        private String host;

        private String key;
    }
}
