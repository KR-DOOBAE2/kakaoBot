package kr.jeommechurial.app;

import kr.jeommechurial.service.OfficeLunchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.*;

@Component
public class LunchRecommendationScheduler {
    private static final Logger log=LoggerFactory.getLogger(LunchRecommendationScheduler.class);
    private static final ZoneId ZONE=ZoneId.of("Asia/Seoul");
    private final OfficeLunchService service; private final KakaoLunchNotifier notifier;
    public LunchRecommendationScheduler(OfficeLunchService service,KakaoLunchNotifier notifier){this.service=service;this.notifier=notifier;}
    @Scheduled(fixedDelayString="${app.lunch-scheduler-delay-ms:60000}")
    public void sendAtLunch(){try{var status=service.today();if(LocalTime.now(ZONE).isBefore(status.profile().lunchTime())||service.notificationSent(status.date())||!notifier.ready())return;var result=service.recommend();var place=result.winner().orElseThrow();var users=service.subscribers();if(users.isEmpty())return;if(notifier.send(place,users))service.markNotificationSent(status.date());}catch(IllegalStateException e){log.debug("Lunch recommendation waiting for setup: {}",e.getMessage());}catch(Exception e){log.warn("Unable to send Kakao lunch recommendation",e);}}
}
