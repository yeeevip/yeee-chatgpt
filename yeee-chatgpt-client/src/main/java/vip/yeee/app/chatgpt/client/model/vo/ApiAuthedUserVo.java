package vip.yeee.app.chatgpt.client.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import vip.yeee.memo.common.appauth.client.model.ApiAuthedUser;

/**
 * description......
 *
 * @author yeeee
 * @since 2024/1/12 11:56
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ApiAuthedUserVo extends ApiAuthedUser {

    private String jscode;

    private String ip;
}
