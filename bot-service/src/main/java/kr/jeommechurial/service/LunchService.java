package kr.jeommechurial.service;

import kr.jeommechurial.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
public class LunchService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private final RoundRepository repository;
    private final SecureRandom random = new SecureRandom();
    private final byte[] ownerSecret;
    private final Clock clock;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    @org.springframework.beans.factory.annotation.Autowired
    public LunchService(RoundRepository repository, @org.springframework.beans.factory.annotation.Value("${app.owner-secret}") String ownerSecret) {
        this(repository,ownerSecret,Clock.systemUTC());
    }
    LunchService(RoundRepository repository,String ownerSecret,Clock clock){this.repository=repository;this.ownerSecret=ownerSecret.getBytes(StandardCharsets.UTF_8);this.clock=clock;}
    @Transactional public CreatedRound create(String title, String roomName) {
        return createRound(title, roomName, null, null, null);
    }
    @Transactional public CreatedRound create(String title, String roomName, String officeAddress,
                                              LocalTime lunchStartTime, LocalTime lunchEndTime) {
        String address=clean(officeAddress,160);
        if(lunchStartTime==null||lunchEndTime==null||!lunchEndTime.isAfter(lunchStartTime))
            throw new IllegalArgumentException("점심 종료 시간은 시작 시간보다 늦어야 해요.");
        return createRound(title,roomName,address,lunchStartTime,lunchEndTime);
    }
    private CreatedRound createRound(String title,String roomName,String address,LocalTime start,LocalTime end) {
        UUID owner=UUID.randomUUID(); String join=token(); LunchRound r=repository.create(owner,clean(title,80),clean(roomName,80),hash(join),nextDrawAt(clock.instant()),address,start,end);
        return new CreatedRound(r.id(),owner,r.title(),r.roomName(),join,ownerToken(r.id()));
    }
    @Transactional public RoundView start(UUID id,String token){requireOwner(id,token);drawIfDue(id);return view(id);}
    @Transactional public RoundView register(UUID id,String token,UUID participantId,String name,String menu){validateJoin(id,token);rolloverIfNeeded(id);requireOpen(id);String cleaned=clean(menu,80);List<MenuEntry> mine=repository.entries(id).stream().filter(e->e.participantId().equals(participantId)).toList();if(mine.stream().noneMatch(e->e.menu().equalsIgnoreCase(cleaned))&&mine.size()>=20)throw new IllegalArgumentException("한 사람은 메뉴를 최대 20개까지 등록할 수 있어요.");repository.addEntry(id,participantId,"익명",cleaned);return view(id);}
    @Transactional public RoundView cancel(UUID id,String token,UUID participantId){validateJoin(id,token);rolloverIfNeeded(id);requireOpen(id);repository.deleteEntry(id,participantId);return view(id);}
    @Transactional public RoundView cancel(UUID id,String token,UUID participantId,UUID entryId){validateJoin(id,token);rolloverIfNeeded(id);requireOpen(id);repository.deleteEntry(id,participantId,entryId);return view(id);}
    @Transactional public RoundView get(UUID id,String token){validateJoin(id,token);rolloverIfNeeded(id);drawIfDue(id);return view(id);}
    @Transactional public RoundView updateSettings(UUID id,String ownerToken,String address,LocalTime start,LocalTime end){
        LunchRound round=requireOwner(id,ownerToken);
        String cleaned=clean(address,160);
        if(start==null||end==null||!end.isAfter(start))throw new IllegalArgumentException("점심 종료 시간은 시작 시간보다 늦어야 해요.");
        if(!repository.updateSettings(id,cleaned,start,end,round.version()))throw new IllegalStateException("다른 변경이 먼저 저장됐어요. 새로고침 후 다시 시도해주세요.");
        return view(id);
    }
    @Transactional public RoundView draw(UUID id,String token,boolean reroll){LunchRound r=requireOwner(id,token);if(r.drawAt()!=null){if(reroll)throw new IllegalStateException("예약된 룰렛 결과는 다시 뽑을 수 없어요.");if(clock.instant().isBefore(r.drawAt()))throw new IllegalStateException("룰렛은 오전 10시 45분에 공개돼요.");drawIfDue(id);return view(id);}if(r.status()!=RoundStatus.OPEN&&!(reroll&&r.status()==RoundStatus.DRAWN))throw new IllegalStateException("현재 상태에서는 추첨할 수 없어요.");List<String> c=new ArrayList<>(repository.candidates(id));if(reroll&&r.winner()!=null)c.removeIf(r.winner()::equalsIgnoreCase);if(c.isEmpty())throw new IllegalStateException("추첨할 메뉴가 없어요.");String winner=c.get(random.nextInt(c.size()));if(!repository.draw(id,winner,r.version(),reroll))throw new IllegalStateException("다른 요청이 먼저 상태를 변경했어요. 새로고침 후 다시 시도해주세요.");return view(id);}
    @Transactional public RoundView close(UUID id,String token){LunchRound r=requireOwner(id,token);if(r.drawAt()!=null)throw new IllegalStateException("익명 메뉴 모집은 오전 10시 45분에 자동 마감돼요.");if(!repository.close(id,r.version()))throw new IllegalStateException("다른 요청이 먼저 상태를 변경했어요. 새로고침 후 다시 시도해주세요.");return view(id);}
    @Transactional public void drawDueOpenRounds(){for(LunchRound round:repository.findDueOpen(clock.instant()))drawIfDue(round.id());}
    public String participantFingerprint(String id,String token){validateJoin(UUID.fromString(id),token);return hash(token);}
    private LunchRound requireOwner(UUID id,String token){LunchRound r=repository.find(id).orElseThrow(()->new NoSuchElementException("방을 찾을 수 없어요."));if(token==null||!MessageDigest.isEqual(ownerToken(id).getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8)))throw new SecurityException("방장 토큰이 올바르지 않아요.");return r;}
    private String ownerToken(UUID id){return id+"."+hmac(id.toString());}
    private String hmac(String value){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(ownerSecret,"HmacSHA256"));return HexFormat.of().formatHex(m.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private void validateJoin(UUID id,String token){LunchRound r=repository.find(id).orElseThrow(()->new NoSuchElementException("방을 찾을 수 없어요."));if(token==null||!MessageDigest.isEqual(hash(token).getBytes(StandardCharsets.UTF_8),r.joinTokenHash().getBytes(StandardCharsets.UTF_8)))throw new SecurityException("참여 링크가 올바르지 않아요.");}
    private void requireOpen(UUID id){LunchRound r=repository.find(id).orElseThrow();if(r.status()!=RoundStatus.OPEN||r.drawAt()!=null&&!clock.instant().isBefore(r.drawAt()))throw new IllegalStateException("메뉴 입력은 오전 10시 44분까지예요. 룰렛 결과를 기다려주세요.");}
    private void rolloverIfNeeded(UUID id){LunchRound r=repository.find(id).orElseThrow();if(r.drawAt()==null)return;LocalDate ballotDay=r.drawAt().atZone(SEOUL).toLocalDate();LocalDate today=clock.instant().atZone(SEOUL).toLocalDate();if(today.isAfter(ballotDay)){if(!repository.resetForNewDay(id,nextDrawAt(clock.instant()),r.version()))throw new IllegalStateException("새 점심 모집을 준비 중이에요. 잠시 후 새로고침해주세요.");}}
    private void drawIfDue(UUID id){LunchRound r=repository.find(id).orElseThrow();if(r.status()!=RoundStatus.OPEN||r.drawAt()==null||clock.instant().isBefore(r.drawAt()))return;List<String> choices=repository.candidates(id);if(choices.isEmpty()){repository.close(id,r.version());return;}String winner=choices.get(random.nextInt(choices.size()));repository.draw(id,winner,r.version(),false);}
    private RoundView view(UUID id){LunchRound r=repository.find(id).orElseThrow(()->new NoSuchElementException("방을 찾을 수 없어요."));List<MenuEntry> anonymous=repository.entries(id).stream().map(e->new MenuEntry(e.id(),e.roundId(),e.participantId(),"익명",e.menu(),e.createdAt(),e.updatedAt())).toList();return new RoundView(id,r.title(),r.roomName(),r.status(),r.winner(),anonymous,repository.candidates(id),r.createdAt(),r.updatedAt(),r.drawAt(),r.officeAddress(),r.lunchStartTime(),r.lunchEndTime());}
    static Instant nextDrawAt(Instant now){ZonedDateTime local=now.atZone(SEOUL);ZonedDateTime draw=local.toLocalDate().atTime(10,45).atZone(SEOUL);return local.isBefore(draw)?draw.toInstant():draw.plusDays(1).toInstant();}
    private String token(){byte[] b=new byte[24];random.nextBytes(b);StringBuilder s=new StringBuilder();for(byte v:b)s.append(ALPHABET.charAt(Byte.toUnsignedInt(v)%ALPHABET.length()));return s.toString();}
    private static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static String clean(String s,int n){if(s==null||s.isBlank())throw new IllegalArgumentException("필수 입력을 확인해주세요.");s=s.strip();if(s.length()>n)throw new IllegalArgumentException("입력이 너무 길어요.");return s;}
}
