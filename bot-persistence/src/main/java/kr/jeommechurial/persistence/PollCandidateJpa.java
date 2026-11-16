package kr.jeommechurial.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public interface PollCandidateJpa extends JpaRepository<PollCandidateEntity,UUID> {
    List<PollCandidateEntity> findByPollDateOrderByDisplayOrderAsc(LocalDate date);
    void deleteByPollDate(LocalDate date);
}
