package kr.jeommechurial.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "kr.jeommechurial")
@EntityScan("kr.jeommechurial.persistence")
@EnableJpaRepositories("kr.jeommechurial.persistence")
@EnableScheduling
public class JeommechurialApplication {
    public static void main(String[] args) { SpringApplication.run(JeommechurialApplication.class, args); }
}

