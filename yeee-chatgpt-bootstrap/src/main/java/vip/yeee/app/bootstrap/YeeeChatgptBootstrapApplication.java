package vip.yeee.app.bootstrap;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableMethodCache(basePackages = "vip.yeee.app")
@EnableCreateCacheAnnotation
@SpringBootApplication
@ComponentScan({"vip.yeee.app"})
public class YeeeChatgptBootstrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(YeeeChatgptBootstrapApplication.class, args);
    }

}
