package kr.jeommechurial.service;

import kr.jeommechurial.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

@Service
public class LunchService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private final RoundRepository repository;
    private final SecureRandom random = new SecureRandom();
    private final byte[] ownerSecret;
    public LunchService(RoundRepository repository, @org.springframework.beans.factory.annotation.Value("${app.owner-secret}") String ownerSecret) {
        this.repository=repository; this.ownerSecret=ownerSecret.getBytes(StandardCharsets.UTF_8);
    }
    @Transactional public CreatedRound create(String title, String roomName) {
        UUID owner=UUID.randomUUID(); String join=token(); LunchRound r=repository.create(owner,clean(title,80),clean(roomName,80),hash(join));
        return new CreatedRound(r.id(),owner,r.title(),r.roomName(),join,ownerToken(r.id()));
    }
    @Transactional public RoundView start(UUID id,String token){requireOwner(id,token);return view(id);}
    @Transactional public RoundView register(UUID id,String token,UUID participantId,String name,String menu){validateJoin(id,token);requireOpen(id);repository.upsertEntry(id,participantId,clean(name,40),clean(menu,80));return view(id);}
    @Transactional public RoundView cancel(UUID id,String token,UUID participantId){validateJoin(id,token);requireOpen(id);repository.deleteEntry(id,participantId);return view(id);}
    @Transactional(readOnly=true) public RoundView get(UUID id,String token){validateJoin(id,token);return view(id);}
    @Transactional public RoundView draw(UUID id,String token,boolean reroll){LunchRound r=requireOwner(id,token);if(r.status()!=RoundStatus.OPEN&&!(reroll&&r.status()==RoundStatus.DRAWN))throw new IllegalStateException("현재 상태에서는 추첨할 수 없어요.");List<String> c=new ArrayList<>(repository.candidates(id));if(reroll&&r.winner()!=null)c.removeIf(r.winner()::equalsIgnoreCase);if(c.isEmpty())throw new IllegalStateException("추첨할 메뉴가 없어요.");String winner=c.get(random.nextInt(c.size()));if(!repository.draw(id,winner,r.version()))throw new IllegalStateException("다른 요청이 먼저 상태를 변경했어요. 새로고침 후 다시 시도해주세요.");return view(id);}
    @Transactional public RoundView close(UUID id,String token){LunchRound r=requireOwner(id,token);if(!repository.close(id,r.version()))throw new IllegalStateException("다른 요청이 먼저 상태를 변경했어요. 새로고침 후 다시 시도해주세요.");return view(id);}
    public String participantFingerprint(String id,String token){validateJoin(UUID.fromString(id),token);return hash(token);}
    private LunchRound requireOwner(UUID id,String token){LunchRound r=repository.find(id).orElseThrow(()->new NoSuchElementException("방을 찾을 수 없어요."));if(token==null||!MessageDigest.isEqual(ownerToken(id).getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8)))throw new SecurityException("방장 토큰이 올바르지 않아요.");return r;}
    private String ownerToken(UUID id){return id+"."+hmac(id.toString());}
    private String hmac(String value){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(ownerSecret,"HmacSHA256"));return HexFormat.of().formatHex(m.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private void validateJoin(UUID id,String token){LunchRound r=repository.find(id).orElseThrow(()->new NoSuchElementException("방을 찾을 수 없어요."));if(token==null||!MessageDigest.isEqual(hash(token).getBytes(StandardCharsets.UTF_8),r.joinTokenHash().getBytes(StandardCharsets.UTF_8)))throw new SecurityException("참여 링크가 올바르지 않아요.");}
    private void requireOpen(UUID id){if(repository.find(id).orElseThrow().status()!=RoundStatus.OPEN)throw new IllegalStateException("모집이 종료됐어요.");}
    private RoundView view(UUID id){LunchRound r=repository.find(id).orElseThrow(()->new NoSuchElementException("방을 찾을 수 없어요."));return new RoundView(id,r.title(),r.roomName(),r.status(),r.winner(),repository.entries(id),repository.candidates(id),r.createdAt(),r.updatedAt());}
    private String token(){byte[] b=new byte[24];random.nextBytes(b);StringBuilder s=new StringBuilder();for(byte v:b)s.append(ALPHABET.charAt(Byte.toUnsignedInt(v)%ALPHABET.length()));return s.toString();}
    private static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static String clean(String s,int n){if(s==null||s.isBlank())throw new IllegalArgumentException("필수 입력을 확인해주세요.");s=s.strip();if(s.length()>n)throw new IllegalArgumentException("입력이 너무 길어요.");return s;}
}
