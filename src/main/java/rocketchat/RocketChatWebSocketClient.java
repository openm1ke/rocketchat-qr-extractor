package rocketchat;

import lombok.Getter;
import lombok.Setter;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class RocketChatWebSocketClient extends WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(RocketChatWebSocketClient.class);

    @Setter
    private String token;
    @Setter
    private String botUserName;
    @Getter
    private String responseString;
    @Getter
    private boolean finished = false;

    private static final String ENTER_COMMAND = "/enter";
    private static final String EXPECTED_MSG = "The QR code is valid for 30 seconds after creation";

    public RocketChatWebSocketClient(String webSocketUri) {
        super(URI.create(webSocketUri));
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        logger.info("Connected to Rocket.Chat");
        connectByToken(token);
    }

    @Override
    public void onMessage(String msg) {
        logger.debug("Received message: " + msg);
        JSONObject jsonMessage = new JSONObject(msg);
        String msgType = jsonMessage.getString("msg");

        if("result".equals(msgType)) {
            String id = jsonMessage.getString("id");
            if("42".equals(id)) {
                if(jsonMessage.has("error")) {
                    responseString = "Login failed: " + jsonMessage.getJSONObject("error").getString("message");
                    closeConnection();
                } else {
                    openDirectMessage(botUserName);
                }
            } else if("unique_create_dm_id".equals(id)) {
                if (jsonMessage.has("error")) {
                    responseString = "Failed to create direct message: " + jsonMessage.getJSONObject("error").getString("message");
                    closeConnection();
                } else {
                    JSONObject result = jsonMessage.getJSONObject("result");
                    if(result != null) {
                        String roomId = result.optString("rid");
                        logger.debug("Room ID for direct message with " + botUserName + ": " + roomId);
                        subscribeToRoomMessages(roomId);
                        sendCommand(roomId, ENTER_COMMAND);
                    }
                }
            }
        } else if ("changed".equals(msgType) && "stream-room-messages".equals(jsonMessage.optString("collection"))) {
            JSONObject fields = jsonMessage.optJSONObject("fields");
            if (fields != null) {
                JSONObject args = fields.optJSONArray("args").optJSONObject(0);
                if(args != null) {
                    String receivedMessageText = args.optString("msg");
                    // Проверяем, что это ответ на наш запрос
                    if (receivedMessageText.contains(EXPECTED_MSG)) {
                        responseString = receivedMessageText;
                        logger.debug("Received message: " + receivedMessageText);
                        closeConnection();
                    }
                }
            }
        } else if ("ping".equals(msgType)) {
            // Ответ на ping
            logger.debug("Received ping");
            send(new JSONObject().put("msg", "pong").toString());
            logger.debug("Sent pong");
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        synchronized (this) {
            finished = true;
            notifyAll();
        }
    }

    @Override
    public void onError(Exception e) {
        responseString = "Error: " + e.getMessage();
        logger.error("Error: " + e.getMessage());
        closeConnection();
    }

    private void connectByToken(String token) {
        JSONObject connectMessage = new JSONObject();
        connectMessage.put("msg", "connect");
        connectMessage.put("version", "1");
        connectMessage.put("support", new String[]{"1", "pre2", "pre1"});
        send(connectMessage.toString());

        // Сообщение для авторизации
        JSONObject loginMessage = new JSONObject();
        loginMessage.put("msg", "method");
        loginMessage.put("method", "login");
        loginMessage.put("id", "42");

        JSONObject params = new JSONObject();
        params.put("resume", token);

        loginMessage.put("params", new JSONObject[]{params});
        send(loginMessage.toString());
    }

    // Метод для подписки на сообщения из комнаты
    private void subscribeToRoomMessages(String roomId) {
        logger.debug("Subscribing to room messages: " + roomId);
        JSONObject subscribeMessage = new JSONObject();
        subscribeMessage.put("msg", "sub");
        subscribeMessage.put("id", "unique_subscription_id");
        subscribeMessage.put("name", "stream-room-messages");
        subscribeMessage.put("params", new Object[]{roomId, true});  // `false` означает получение только текстовых сообщений
        send(subscribeMessage.toString());
    }

    // Метод для открытия диалога с пользователем по нику и получения room ID
    private void openDirectMessage(String username) {
        // Создаем объект для отправки запроса на создание личного чата
        JSONObject createDMMessage = new JSONObject();
        createDMMessage.put("msg", "method");
        createDMMessage.put("method", "createDirectMessage");
        createDMMessage.put("id", "unique_create_dm_id");

        // Параметры запроса: имя пользователя, с которым нужно открыть чат
        createDMMessage.put("params", new JSONArray().put(username));

        // Отправляем запрос на сервер
        send(createDMMessage.toString());
    }

    // Метод для отправки команды через WebSocket
    private void sendCommand(String roomId, String command) {
        // Создаем объект с данными команды
        logger.debug("Sending command: " + command + " to room: " + roomId);
        JSONObject commandObject = new JSONObject();
        commandObject.put("msg", "method");
        commandObject.put("method", "slashCommand");
        commandObject.put("id", "unique_command_id");

        // Устанавливаем параметры команды
        JSONObject paramsObject = new JSONObject();
        paramsObject.put("cmd", command.substring(1)); // Убираем первый символ "/" из команды
        paramsObject.put("params", ""); // Дополнительные параметры, если они есть
        paramsObject.put("msg", new JSONObject().put("rid", roomId)); // Указываем roomId

        commandObject.put("params", new JSONObject[]{paramsObject});

        // Отправляем команду на сервер
        send(commandObject.toString());
    }

    // Метод для закрытия соединения
    private void closeConnection() {
        if (!finished) {
            logger.debug("Connection already closed");
            close(); // Закрывает WebSocket соединение
        }
    }
}
