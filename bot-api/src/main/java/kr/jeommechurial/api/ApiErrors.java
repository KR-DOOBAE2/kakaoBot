package kr.jeommechurial.api;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(SecurityException.class) ResponseEntity<?> forbidden(SecurityException e){return ResponseEntity.status(403).body(Map.of("error",e.getMessage()));}
 @ExceptionHandler(java.util.NoSuchElementException.class) ResponseEntity<?> missing(RuntimeException e){return ResponseEntity.status(404).body(Map.of("error",e.getMessage()));}
 @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class,org.springframework.web.bind.MethodArgumentNotValidException.class}) ResponseEntity<?> bad(Exception e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()==null?"입력값을 확인해주세요.":e.getMessage()));}
}
