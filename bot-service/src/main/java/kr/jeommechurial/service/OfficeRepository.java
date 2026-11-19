package kr.jeommechurial.service;

import kr.jeommechurial.core.OfficeProfile;
import kr.jeommechurial.core.PlaceCandidate;
import kr.jeommechurial.core.PlaceSearchResult;
import kr.jeommechurial.core.Satisfaction;
import java.time.LocalDate;
import java.util.*;

public interface OfficeRepository {
    Optional<OfficeProfile> profile();
    void saveProfile(OfficeProfile profile);
    List<PlaceCandidate> discover(List<PlaceSearchResult> places);
    void savePoll(LocalDate date, List<String> placeIds);
    List<PlaceCandidate> pollCandidates(LocalDate date);
    boolean hasVoted(LocalDate date, UUID participantId);
    void vote(LocalDate date, UUID participantId, String placeId);
    Map<String, Long> votes(LocalDate date);
    void rate(UUID participantId, String placeId, Satisfaction satisfaction);
    Optional<String> winner(LocalDate date);
    void setWinner(LocalDate date, String placeId);
    void subscribe(String botUserKey);
    void unsubscribe(String botUserKey);
    List<String> subscribers();
    boolean notificationSent(LocalDate date);
    void markNotificationSent(LocalDate date);
}

