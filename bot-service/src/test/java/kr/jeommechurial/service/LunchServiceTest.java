package kr.jeommechurial.service;

import kr.jeommechurial.core.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LunchServiceTest {
    private FakeRepository repo; private LunchService service;
    @BeforeEach void setup(){repo=new FakeRepository();service=new LunchService(repo,"test-secret");}
    @Test void duplicateMenusCollapseAndRerollExcludesPreviousWinner(){CreatedRound r=service.create("점심","우리방");UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID();service.register(r.id(),r.joinToken(),a,"A","김치찌개");service.register(r.id(),r.joinToken(),b,"B","김치찌개");service.register(r.id(),r.joinToken(),c,"C","돈까스");assertEquals(2,repo.candidates(r.id()).size());RoundView first=service.draw(r.id(),r.ownerToken(),false);RoundView second=service.draw(r.id(),r.ownerToken(),true);assertNotEquals(first.winner(),second.winner());}
    @Test void participantCanChangeAndCancelOnlyTheirOwnEntry(){CreatedRound r=service.create("점심","우리방");UUID a=UUID.randomUUID(),b=UUID.randomUUID();service.register(r.id(),r.joinToken(),a,"A","국밥");service.register(r.id(),r.joinToken(),a,"A","냉면");service.register(r.id(),r.joinToken(),b,"B","국밥");service.cancel(r.id(),r.joinToken(),a);RoundView v=service.get(r.id(),r.joinToken());assertEquals(1,v.entries().size());assertEquals("국밥",v.entries().getFirst().menu());}
    @Test void rejectsBadTokensAndRegistrationAfterClose(){CreatedRound r=service.create("점심","우리방");assertThrows(SecurityException.class,()->service.get(r.id(),"wrong"));service.close(r.id(),r.ownerToken());assertThrows(IllegalStateException.class,()->service.register(r.id(),r.joinToken(),UUID.randomUUID(),"A","국밥"));}
    static class FakeRepository implements RoundRepository {
        final Map<UUID,LunchRound> rounds=new HashMap<>();final Map<UUID,List<MenuEntry>> entries=new HashMap<>();
        public LunchRound create(UUID owner,String title,String room,String hash){UUID id=UUID.randomUUID();LunchRound r=new LunchRound(id,owner,title,room,hash,RoundStatus.OPEN,null,0,Instant.now(),Instant.now());rounds.put(id,r);entries.put(id,new ArrayList<>());return r;}
        public Optional<LunchRound> find(UUID id){return Optional.ofNullable(rounds.get(id));}
        public List<MenuEntry> entries(UUID id){return List.copyOf(entries.get(id));}
        public MenuEntry upsertEntry(UUID rid,UUID pid,String name,String menu){List<MenuEntry> l=entries.get(rid);l.removeIf(e->e.participantId().equals(pid));MenuEntry e=new MenuEntry(UUID.randomUUID(),rid,pid,name,menu,Instant.now(),Instant.now());l.add(e);return e;}
        public void deleteEntry(UUID rid,UUID pid){entries.get(rid).removeIf(e->e.participantId().equals(pid));}
        public List<String> candidates(UUID id){return entries.get(id).stream().map(e->e.menu().strip().toLowerCase(Locale.ROOT)).distinct().toList();}
        public boolean draw(UUID id,String winner,long version){LunchRound r=rounds.get(id);if(r.version()!=version)return false;rounds.put(id,new LunchRound(id,r.ownerId(),r.title(),r.roomName(),r.joinTokenHash(),RoundStatus.DRAWN,winner,version+1,r.createdAt(),Instant.now()));return true;}
        public boolean close(UUID id,long version){LunchRound r=rounds.get(id);if(r.version()!=version)return false;rounds.put(id,new LunchRound(id,r.ownerId(),r.title(),r.roomName(),r.joinTokenHash(),RoundStatus.CLOSED,r.winner(),version+1,r.createdAt(),Instant.now()));return true;}
    }
}
