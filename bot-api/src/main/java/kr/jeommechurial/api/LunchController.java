package kr.jeommechurial.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.jeommechurial.core.*;
import kr.jeommechurial.service.LunchService;
import kr.jeommechurial.service.RoundRestaurantService;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;

@RestController @RequestMapping("/api")
public class LunchController {
    private final LunchService service;
    private final RoundRestaurantService restaurants;
    public LunchController(LunchService service,RoundRestaurantService restaurants){this.service=service;this.restaurants=restaurants;}
    public record CreateRequest(@NotBlank @Size(max=80) String title,@NotBlank @Size(max=80) String roomName,
                                @NotBlank @Size(max=160) String officeAddress,
                                @NotNull LocalTime lunchStartTime,@NotNull LocalTime lunchEndTime){}
    public record MenuRequest(@NotBlank @Size(max=80) String menu){}
    public record SettingsRequest(@NotBlank @Size(max=160) String officeAddress,
                                  @NotNull LocalTime lunchStartTime,@NotNull LocalTime lunchEndTime){}
    public record RoundResponse(String id,String title,String roomName,String status,String winner,Instant drawAt,
                                String officeAddress,LocalTime lunchStartTime,LocalTime lunchEndTime,
                                List<Entry> entries,List<String> candidates,boolean admin,String participantId){}
    public record Entry(String id,String menu,boolean mine){}
    @PostMapping("/rounds") public Map<String,Object> create(@Valid @RequestBody CreateRequest req,HttpServletRequest request){CreatedRound r=service.create(req.title(),req.roomName(),req.officeAddress(),req.lunchStartTime(),req.lunchEndTime());String base=origin(request);return Map.of("id",r.id(),"title",r.title(),"roomName",r.roomName(),"joinUrl",base+"/r/"+r.id()+"?token="+r.joinToken(),"joinToken",r.joinToken(),"ownerToken",r.ownerToken());}
    @GetMapping("/rounds/{id}") public RoundResponse get(@PathVariable("id") UUID id,@RequestParam("token") String token,@RequestHeader(value="X-Participant-Id",required=false) String participant){return response(service.get(id,token),false,validParticipant(participant).toString());}
    @GetMapping("/rounds/{id}/restaurants") public RoundRestaurantService.Recommendation restaurants(@PathVariable("id") UUID id,@RequestParam("token") String token){return restaurants.recommend(id,token);}
    @PutMapping("/rounds/{id}/settings") public RoundResponse settings(@PathVariable("id") UUID id,@RequestHeader("X-Owner-Token") String token,@Valid @RequestBody SettingsRequest req){return response(service.updateSettings(id,token,req.officeAddress(),req.lunchStartTime(),req.lunchEndTime()),true,null);}
    @PostMapping("/rounds/{id}/start") public RoundResponse start(@PathVariable("id") UUID id,@RequestHeader("X-Owner-Token") String token){return response(service.start(id,token),true,null);}
    @PutMapping("/rounds/{id}/entries") public RoundResponse upsert(@PathVariable("id") UUID id,@RequestParam("token") String token,@RequestHeader(value="X-Participant-Id",required=false) String participant,@Valid @RequestBody MenuRequest req){UUID pid=validParticipant(participant);return response(service.register(id,token,pid,null,req.menu()),false,pid.toString());}
    @DeleteMapping("/rounds/{id}/entries/me") public RoundResponse cancel(@PathVariable("id") UUID id,@RequestParam("token") String token,@RequestHeader("X-Participant-Id") String participant){return response(service.cancel(id,token,UUID.fromString(participant)),false,participant);}
    @DeleteMapping("/rounds/{id}/entries/{entryId}") public RoundResponse cancelOne(@PathVariable("id") UUID id,@PathVariable("entryId") UUID entryId,@RequestParam("token") String token,@RequestHeader("X-Participant-Id") String participant){return response(service.cancel(id,token,UUID.fromString(participant),entryId),false,participant);}
    @PostMapping("/rounds/{id}/draw") public RoundResponse draw(@PathVariable("id") UUID id,@RequestHeader("X-Owner-Token") String token){return response(service.draw(id,token,false),true,null);}
    @PostMapping("/rounds/{id}/reroll") public RoundResponse reroll(@PathVariable("id") UUID id,@RequestHeader("X-Owner-Token") String token){return response(service.draw(id,token,true),true,null);}
    @PostMapping("/rounds/{id}/close") public RoundResponse close(@PathVariable("id") UUID id,@RequestHeader("X-Owner-Token") String token){return response(service.close(id,token),true,null);}
    private static UUID validParticipant(String value){try{return value==null?UUID.randomUUID():UUID.fromString(value);}catch(Exception e){return UUID.randomUUID();}}
    private static RoundResponse response(RoundView r,boolean admin,String participant){return new RoundResponse(r.id().toString(),r.title(),r.roomName(),r.status().name(),r.winner(),r.drawAt(),r.officeAddress(),r.lunchStartTime(),r.lunchEndTime(),r.entries().stream().map(e->new Entry(e.id().toString(),e.menu(),participant!=null&&e.participantId().toString().equals(participant))).toList(),r.candidates(),admin,participant);}
    private static String origin(HttpServletRequest r){String scheme=r.getHeader("X-Forwarded-Proto");String host=r.getHeader("X-Forwarded-Host");return (scheme==null?r.getScheme():scheme)+"://"+(host==null?r.getHeader("Host"):host);}
}


