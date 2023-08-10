package vip.yeee.app.chatgpt.client.job;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vip.yeee.app.chatgpt.client.domain.local.ChatLocalRepository;
import vip.yeee.app.chatgpt.client.listener.AbstractStreamListener;
import vip.yeee.app.chatgpt.client.listener.ChatAppWsContext;

import vip.yeee.memo.common.websocket.netty.bootstrap.Session;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * description......
 *
 * @author yeeee
 * @since 2023/4/15 10:11
 */
@Slf4j
@Component
public class AppJobs {

    @Value("${yeee.lexicon.sensitive.path}")
    private String sensitiveLexiconPath;

//    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    public void scheRefreshUserSession() {
        List<Session> sessionList = ChatAppWsContext.allUserSession();
//        log.info("【Chat任务】- 定时session心跳，sessionSize = {}", sessionList.size());
        for (Session session : sessionList) {
            try {
                if (session == null || !session.isOpen()) {
                    return;
                }
//                log.info("【Chat任务】- 定时session心跳，发送心跳 session = {}", session);
                AbstractStreamListener.Message msg = new AbstractStreamListener.Message();
                msg.setMsgId("");
                msg.setKind("heart");
                msg.setMsg("");
//                msg.setCreateTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
                session.sendText(JSON.toJSONString(msg));
            } catch (Exception e) {
                log.error("【Chat任务】- 定时session心跳，失败", e);
            }
        }
    }

    @Component
    public class LoadSensitiveLexicon2Cache implements ApplicationRunner {
        @Override
        public void run(ApplicationArguments args) throws Exception {
            try {
                Stopwatch stopwatch = Stopwatch.createStarted();
                log.info("【LoadSensitiveLexicon2Cache】- 开始");
                List<String> stringList = FileUtil.readLines(sensitiveLexiconPath, StandardCharsets.UTF_8);
                stringList.forEach(ChatLocalRepository::addSensWord);
                log.info("【LoadSensitiveLexicon2Cache】- 结束，耗时：{}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            } catch (Exception e) {
                log.warn("【LoadSensitiveLexicon2Cache】- 失败", e);
            }
        }
    }

}
