package kr.jeommechurial.persistence;

import kr.jeommechurial.core.*;
import kr.jeommechurial.service.RoundRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;

@Repository @Transactional
class PostgresRoundRepository implements RoundRepository {
    private final RoundJpa rounds; private final MenuJpa menus;
    @PersistenceContext private EntityManager entityManager;
    PostgresRoundRepository(RoundJpa rounds,MenuJpa menus){this.rounds=rounds;this.menus=menus;}
    public LunchRound create(UUID owner,String title,String room,String tokenHash,Instant drawAt,String officeAddress,LocalTime lunchStartTime,LocalTime lunchEndTime){RoundEntity e=new RoundEntity();e.ownerId=owner;e.title=title;e.roomName=room;e.joinTokenHash=tokenHash;e.drawAt=drawAt;e.officeAddress=officeAddress;e.lunchStartTime=lunchStartTime;e.lunchEndTime=lunchEndTime;return model(rounds.save(e));}
    @Transactional(readOnly=true) public Optional<LunchRound> find(UUID id){return rounds.findById(id).map(PostgresRoundRepository::model);}
    @Transactional(readOnly=true) public List<LunchRound> findDueOpen(Instant now){return rounds.findByStatusAndDrawAtLessThanEqual("OPEN",now).stream().map(PostgresRoundRepository::model).toList();}
    @Transactional(readOnly=true) public List<MenuEntry> entries(UUID id){return menus.findByRoundIdOrderByCreatedAtAsc(id).stream().map(PostgresRoundRepository::model).toList();}
    public MenuEntry addEntry(UUID rid,UUID pid,String name,String menu){menus.insertIfAbsent(UUID.randomUUID(),rid,pid,name,menu);return model(menus.findByRoundIdAndParticipantIdAndMenuIgnoreCase(rid,pid,menu).orElseThrow());}
    public void deleteEntry(UUID rid,UUID pid){menus.remove(rid,pid);}
    public void deleteEntry(UUID rid,UUID pid,UUID entryId){menus.removeOne(rid,pid,entryId);}
    @Transactional(readOnly=true) public List<String> candidates(UUID id){return menus.findByRoundIdOrderByCreatedAtAsc(id).stream().map(MenuEntity::getMenu).filter(Objects::nonNull).collect(java.util.stream.Collectors.toMap(v->v.strip().toLowerCase(Locale.ROOT),v->v.strip(),(first,ignored)->first,TreeMap::new)).values().stream().toList();}
    public boolean draw(UUID id,String winner,long expected,boolean reroll,Instant drawnAt){int n=entityManager.createNativeQuery("update lunch_rounds set winner=:winner,status='DRAWN',draw_at=:drawAt,version=version+1,updated_at=now() where id=:id and version=:version and status=:status").setParameter("winner",winner).setParameter("drawAt",drawnAt).setParameter("id",id).setParameter("version",expected).setParameter("status",reroll?"DRAWN":"OPEN").executeUpdate();if(n==1)entityManager.clear();return n==1;}
    public boolean close(UUID id,long expected){int n=entityManager.createNativeQuery("update lunch_rounds set status='CLOSED',version=version+1,updated_at=now() where id=:id and version=:version and status<>'CLOSED'").setParameter("id",id).setParameter("version",expected).executeUpdate();if(n==1)entityManager.clear();return n==1;}
    public boolean updateSettings(UUID id,String address,LocalTime start,LocalTime end,long expected){int n=entityManager.createNativeQuery("update lunch_rounds set office_address=:address,lunch_start_time=:start,lunch_end_time=:end,version=version+1,updated_at=now() where id=:id and version=:version").setParameter("address",address).setParameter("start",start).setParameter("end",end).setParameter("id",id).setParameter("version",expected).executeUpdate();if(n==1)entityManager.clear();return n==1;}
    public boolean resetForNewDay(UUID id,Instant drawAt,long expected){int n=entityManager.createNativeQuery("update lunch_rounds set status='OPEN',winner=null,draw_at=:drawAt,version=version+1,updated_at=now() where id=:id and version=:version").setParameter("drawAt",drawAt).setParameter("id",id).setParameter("version",expected).executeUpdate();if(n==1){menus.deleteByRoundId(id);entityManager.clear();}return n==1;}
    private static LunchRound model(RoundEntity e){return new LunchRound(e.id,e.ownerId,e.title,e.roomName,e.joinTokenHash,RoundStatus.valueOf(e.status),e.winner,e.version,e.createdAt,e.updatedAt,e.drawAt,e.officeAddress,e.lunchStartTime,e.lunchEndTime);}
    private static MenuEntry model(MenuEntity e){return new MenuEntry(e.id,e.roundId,e.participantId,e.participantName,e.menu,e.createdAt,e.updatedAt);}
}



