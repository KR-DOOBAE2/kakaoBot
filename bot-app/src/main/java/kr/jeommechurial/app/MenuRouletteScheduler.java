package kr.jeommechurial.app;

import kr.jeommechurial.service.LunchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MenuRouletteScheduler {
    private static final Logger log = LoggerFactory.getLogger(MenuRouletteScheduler.class);
    private final LunchService lunchService;

    public MenuRouletteScheduler(LunchService lunchService) {
        this.lunchService = lunchService;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void resetAtMidnight() {
        catchUp();
    }

    @Scheduled(fixedDelay = 60000)
    public void catchUp() {
        try {
            lunchService.resetPreviousDayMenus();
            lunchService.drawDueOpenRounds();
        } catch (Exception e) {
            log.warn("Unable to finish a scheduled menu roulette", e);
        }
    }
}
