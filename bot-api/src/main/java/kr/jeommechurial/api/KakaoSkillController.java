package kr.jeommechurial.api;

import com.fasterxml.jackson.databind.JsonNode;
import kr.jeommechurial.core.CreatedRound;
import kr.jeommechurial.core.RoundView;
import kr.jeommechurial.service.LunchService;
import kr.jeommechurial.service.OfficeLunchService;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/kakao")
public class KakaoSkillController {
    private static final Pattern COMMAND = Pattern.compile("^/?(#룰렛돌리기|방 만들기|방만들기|메뉴|취소|목록|추첨|재추첨|결과|마감|회사설정|오늘점심|점심추천|투표|좋아요|싫어요|알림켜기|알림끄기|도움말)(?:\\s+(.*))?$", Pattern.DOTALL);
    private final LunchService service;
    private final OfficeLunchService officeService;
    private final String skillSecret;
    private final String publicBaseUrl;

    public KakaoSkillController(LunchService service, OfficeLunchService officeService,
                                @Value("${app.kakao-skill-secret:}") String skillSecret,
                                @Value("${app.public-base-url:}") String publicBaseUrl) {
        this.service = service;
        this.officeService = officeService;
        this.skillSecret = skillSecret;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
    }

    @PostMapping("/skill")
    public Map<String, Object> skill(@RequestBody JsonNode payload,
                                     @RequestHeader(value = "X-Skill-Secret", required = false) String suppliedSecret) {
        if (!skillSecret.isBlank() && !constantTimeEquals(skillSecret, suppliedSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid skill secret");
        }
        String utterance = payload.path("userRequest").path("utterance").asText("").strip();
        String userKey = payload.path("userRequest").path("user").path("id").asText("").strip();
        Matcher matcher = COMMAND.matcher(utterance);
        String reply;
        if (!matcher.matches()) {
            reply = officeService.configured() ? help() : "처음 오셨군요! 점심 시작·종료 시간과 회사주소를 알려주세요.\n예: 회사설정 12:00 13:00 서울 강남구 테헤란로 123";
        } else {
            String command = matcher.group(1);
            String args = matcher.group(2) == null ? "" : matcher.group(2).strip();
            try {
                reply = handle(command, args, userKey);
            } catch (Exception e) {
                reply = e.getMessage() == null ? "요청을 처리하지 못했어요. 입력을 확인하고 다시 시도해주세요." : e.getMessage();
            }
        }
        return Map.of("version", "2.0", "template", Map.of("outputs", List.of(Map.of("simpleText", Map.of("text", reply)))));
    }

    private String handle(String command, String args, String userKey) {
        if (command.equals("알림켜기")) {requireUser(userKey);officeService.subscribe(userKey);return "점심 추천 알림을 켰어요. 점심시간에 이 챗봇 채널로 추천을 보내드릴게요. 채널을 추가해야 알림을 받을 수 있어요.";}
        if (command.equals("알림끄기")) {officeService.unsubscribe(userKey);return "점심 추천 알림을 껐어요.";}
        if (command.equals("회사설정")) {
            String[] split=args.split("\\s+",3);
            if(split.length<3||!split[0].matches("\\d{1,2}:\\d{2}")||!split[1].matches("\\d{1,2}:\\d{2}"))return "사용법: 회사설정 시작시간 종료시간 회사주소\n예: 회사설정 12:00 13:00 서울 강남구 테헤란로 123";
            var p=officeService.configure(split[2],LocalTime.parse(split[0]),LocalTime.parse(split[1]),1000,userKey);
            var poll=officeService.today();
            return "🏢 회사 위치와 점심시간을 저장했어요!\n"+p.address()+" · "+p.lunchTime()+"~"+p.lunchEndTime()+"\n시작 전에 투표하고, 종료 후 식당을 평가해주세요.\n"+pollText(poll);
        }
        if (command.equals("오늘점심")||command.equals("점심추천")) {
            var status=officeService.today();
            if(status.pollOpen())return "🗳️ 점심 투표를 진행 중이에요!\n"+pollText(status)+"\n투표하려면 투표 후보번호를 보내주세요.";
            var result=officeService.recommend();var winner=result.winner().orElseThrow();
            return "🎉 오늘의 식당 추천: "+winner.name()+"\n"+winner.address()+" ("+winner.distanceMeters()+"m)\n"+winner.placeUrl()+"\n최근 발견 → 투표수 → 거리 → 평가 순으로 선정 · 투표 "+result.votes().getOrDefault(winner.placeId(),0L)+"표\n"+result.profile().lunchEndTime()+" 이후 좋아요 또는 싫어요로 평가해주세요.\n"+sourceLabel(winner.placeId());
        }
        if (command.equals("투표")) {
            if(!args.matches("\\d+"))return "투표할 후보 번호를 입력해주세요. 예: 투표 2";
            requireUser(userKey);var status=officeService.vote(Integer.parseInt(args),participantId(userKey));return "투표를 저장했어요!\n"+pollText(status);
        }
        if (command.equals("좋아요")||command.equals("싫어요")) {
            requireUser(userKey);officeService.rate(participantId(userKey),command.equals("좋아요"));return command+" 평가를 저장했어요. 다음 추천에 반영할게요!";
        }
        if (command.equals("도움말")) return help();
        if (command.equals("방 만들기") || command.equals("방만들기")) {
            requireUser(userKey);
            String title = args.isBlank() ? "오늘 점심 메뉴 모집" : args.substring(0, Math.min(args.length(), 80));
            CreatedRound round = service.create(title, "점메추리알 챗봇");
            String joinUrl = publicBaseUrl.isBlank() ? "" : "\n참여 웹 링크: " + publicBaseUrl + "/r/" + round.id() + "?token=" + round.joinToken();
            return "🥚 익명 점심방을 만들었어요!\n메뉴는 오전 10시 44분까지 받고, 10시 45분에 룰렛 결과를 공개해요.\n방 ID: " + round.id() + "\n참여 토큰: " + round.joinToken() + "\n방장 토큰: " + round.ownerToken() + joinUrl
                    + "\n\n참여 명령: 메뉴 " + round.id() + " " + round.joinToken() + " 메뉴이름";
        }
        if (command.equals("메뉴")) {
            String[] p = args.split("\\s+", 3);
            if (p.length < 3) return "사용법: 메뉴 방ID 참여토큰 메뉴이름\n예: 메뉴 ... ... 김치찌개";
            UUID id = UUID.fromString(p[0]);
            RoundView view = service.register(id, p[1], participantId(userKey), null, p[2]);
            return "🍽️ 익명으로 메뉴를 등록했어요!\n현재 후보 " + view.candidates().size() + "개: " + String.join(", ", view.candidates());
        }
        if (command.equals("#룰렛돌리기")) {
            String[] p = args.split("\\s+");
            if (p.length < 2) return "사용법: #룰렛돌리기 방ID 참여토큰\n또는 참여 링크에서 #룰렛돌리기 버튼을 눌러주세요.";
            RoundView view = service.get(UUID.fromString(p[0]), p[1]);
            if (view.winner() == null) return view.status().name().equals("CLOSED") ? "오늘은 등록된 메뉴가 없어 룰렛을 돌리지 못했어요." : "🥚 두구두구… 룰렛 결과는 오전 10시 45분에 공개돼요!";
            String link = publicBaseUrl.isBlank() ? "" : "\n룰렛 애니메이션: " + publicBaseUrl + "/r/" + view.id() + "?token=" + p[1];
            return "🎲 #룰렛돌리기 결과는… " + view.winner() + "! 🎉" + link;
        }
        if (command.equals("취소")) {
            String[] p = args.split("\\s+");
            if (p.length < 2) return "사용법: 취소 방ID 참여토큰";
            RoundView view = service.cancel(UUID.fromString(p[0]), p[1], participantId(userKey));
            return "메뉴 등록을 취소했어요. 남은 후보 " + view.candidates().size() + "개";
        }
        if (command.equals("목록") || command.equals("결과")) {
            String[] p = args.split("\\s+");
            if (p.length < 2) return "사용법: " + command + " 방ID 참여토큰";
            RoundView view = service.get(UUID.fromString(p[0]), p[1]);
            if (command.equals("결과")) return view.winner() == null ? "아직 추첨 결과가 없어요." : "🎉 오늘의 메뉴는 " + view.winner() + " 입니다!";
            String list = view.entries().isEmpty() ? "아직 등록된 메뉴가 없어요." : view.entries().stream().map(e -> "🥚 익명 · " + e.menu()).reduce((a, b) -> a + "\n" + b).orElse("");
            return "🥚 " + view.title() + "\n" + list + "\n\n후보: " + (view.candidates().isEmpty() ? "없음" : String.join(", ", view.candidates()));
        }
        if (command.equals("추첨") || command.equals("재추첨") || command.equals("마감")) {
            String[] p = args.split("\\s+");
            if (p.length < 2) return "사용법: " + command + " 방ID 방장토큰";
            UUID id = UUID.fromString(p[0]);
            if (command.equals("마감")) return "모집을 마감했어요. " + service.close(id, p[1]).title();
            RoundView view = service.draw(id, p[1], command.equals("재추첨"));
            return "🎉 " + (command.equals("재추첨") ? "다시 뽑은" : "오늘의") + " 점심 메뉴는 " + view.winner() + " 입니다!";
        }
        return help();
    }

    private static String pollText(OfficeLunchService.PollStatus s){StringBuilder out=new StringBuilder("회사 주변 식당 후보 (최근 발견 → 거리 → 평가)\n");for(int i=0;i<s.candidates().size();i++){var p=s.candidates().get(i);out.append(i+1).append(". ").append(p.name());if(p.newlyDiscovered())out.append(" 🆕 최근 발견");out.append(" · ").append(p.distanceMeters()).append("m · 평가 ").append(p.score()>=0?"+":"").append(p.score()).append(" · ").append(s.votes().getOrDefault(p.placeId(),0L)).append("표\n");}if(!s.candidates().isEmpty())out.append(sourceLabel(s.candidates().get(0).placeId()));return out.toString().strip();}
    private static String sourceLabel(String placeId){return placeId.startsWith("osm-")?"장소 정보: © OpenStreetMap contributors":"장소 정보: Kakao Map";}
    private static UUID participantId(String userKey) {
        requireUser(userKey);
        return UUID.nameUUIDFromBytes(("kakao-bot-user:" + userKey).getBytes(StandardCharsets.UTF_8));
    }
    private static void requireUser(String userKey) { if (userKey == null || userKey.isBlank()) throw new IllegalArgumentException("카카오 사용자 정보를 확인할 수 없어요."); }
    private static boolean constantTimeEquals(String expected, String actual) { return actual != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8)); }
    private static String help() { return "🥚 점메추리알 명령어\n회사설정 시작시간 종료시간 회사주소\n오늘점심 / 투표 후보번호 / 좋아요 / 싫어요\n방 만들기 [모집이름]\n메뉴 방ID 참여토큰 메뉴이름 (익명)\n목록 방ID 참여토큰\n취소 방ID 참여토큰\n#룰렛돌리기 방ID 참여토큰\n메뉴 모집은 오전 10시 44분까지, 결과는 10시 45분 공개"; }
}







