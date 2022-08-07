package vip.yeee.app.chatgpt.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * description......
 *
 * @author yeeee
 * @since 2023/4/5 20:38
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatMessage2 extends ChatMessage {

    @JsonIgnore
    String magId;
}
