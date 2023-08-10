package vip.yeee.app.chatgpt.client.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import vip.yeee.memo.common.appauth.server.model.vo.JTokenVo;

/**
 * description......
 *
 * @author yeeee
 * @since 2023/8/10 11:50
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserAuthVo extends JTokenVo {

    private String openid;
    private Integer limitCount;
}
