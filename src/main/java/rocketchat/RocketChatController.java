package rocketchat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RocketChatController {

    @Value("${rocket.chat.websocket.uri}")
    private String webSocketUri;
    @Value("${rocket.chat.bot.username}")
    private String botUserName;

    @PostMapping("/qr")
    public ResponseEntity<String> sendCommand(@RequestBody String token) {

        if(token == null) {
            return ResponseEntity.badRequest().body("Token is required");
        }

        RocketChatWebSocketClient rocket = new RocketChatWebSocketClient(webSocketUri);

        rocket.setToken(token);
        rocket.setBotUserName(botUserName);
        rocket.connect();

        try {
            synchronized (rocket) {
                while (!rocket.isFinished()) {
                    rocket.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }

        if(rocket.isFinished()) {
            return ResponseEntity.ok(rocket.getResponseString());
        } else {
            return ResponseEntity.badRequest().body("Error connecting to Rocket.Chat");
        }
    }
}
