package kr.jeommechurial.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.jeommechurial.core.*;
import kr.jeommechurial.service.LunchService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class LunchController {
    private final LunchService service;
    public LunchController(LunchService service){this.service=service;}
    public record CreateRequest(@NotBlank @Size(max=80) String title,@NotBlank @Size(max=80) String roomName){}
    public record MenuRequest(@NotBlank @Size(max=40) String participantName,@NotBlank @Size(max=80) String menu){}
    public record RoundResponse(String id,String title,String roomName,String status,String winner,List<Entry> entries,List<String> candidates,boolean admin,String participantId){}
    public record Entry(String participantName,String menu){}
    @PostMapping("/rounds") public Map<String,Object> create(@Valid @RequestBody CreateRequest req,HttpServletRequest request){CreatedRound r=service.create(req.title(),req.roomName());String base=origin(request);return Map.of("id",r.id(),"title",r.title(),"roomName",r.roomName(),"joinUrl",base+"/r/"+r.id()+"?token="+r.joinToken(),"joinToken",r.joinToken(),"ownerToken",r.ownerToken());}
    @GetMapping("/rounds/{id}") public RoundResponse get(@PathVariable("id") UUID id,@RequestParam("token") String token,@RequestHeader(value="X-Participant-Id",required=false) String participant){return response(service.get(id,token),false,validParticipant(participant).toString());}
    @PostMapping("/rounds/{id}/start") public RoundResponse start(@PathVariable("id") UUID id,@RequestHeader("X-Owner-Token") String token){return response(service.start(id,token),true,null);}
    @PutMapping("/rounds/{id}/entries") public RoundResponse upsert(@PathVariable("id") UUID id,@RequestParam("token") String token,@RequestHeader(value="X-Participant-Id",required=false) String participant,@Valid @RequestBody MenuRequest req){UUID pid=validParticipant(participant);return response(service.register(id,token,pid,req.participantName(),req.menu()),false,pid.toString());}
    @DeleteMapping("/rounds/{id}/entries/me") public RoundResponse cancel(@PathVariable("id") UUID id,@RequestParam("token") String token,@RequestHeader("X-Participant-Id") String participant){return response(service.cancel(id,token,UUID.fromString(participant)),false,participant);}
    @PostMapping("/rounds/{id}/draw") public RoundResponse draw(@PathVariable("id") UUID id,@RequestHeader("X-Owner-Token") String token){return response(service.draw(id,token,false),true,null);}
    @PostMapping("/rounds/{id}/reroll") public RoundResponse reroll(@PathVariable("id") UUID id,@RequestHeader("X-Owner-Token") String token){return response(service.draw(id,token,true),true,null);}
    @PostMapping("/rounds/{id}/close") public RoundResponse close(@PathVariable("id") UUID id,@RequestHeader("X-Owner-Token") String token){return response(service.close(id,token),true,null);}
    private static UUID validParticipant(String value){try{return value==null?UUID.randomUUID():UUID.fromString(value);}catch(Exception e){return UUID.randomUUID();}}
    private static RoundResponse response(RoundView r,boolean admin,String participant){return new RoundResponse(r.id().toString(),r.title(),r.roomName(),r.status().name(),r.winner(),r.entries().stream().map(e->new Entry(e.participantName(),e.menu())).toList(),r.candidates(),admin,participant);}
    private static String origin(HttpServletRequest r){String scheme=r.getHeader("X-Forwarded-Proto");String host=r.getHeader("X-Forwarded-Host");return (scheme==null?r.getScheme():scheme)+"://"+(host==null?r.getHeader("Host"):host);}
}


