package rocketchat;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RocketChatController {
    @PostMapping("/qr")
    public ResponseEntity<String> sendCommand(@RequestBody String token) {
        return ResponseEntity.ok(token.toUpperCase());
    }
}
