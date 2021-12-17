package chat.server;

public interface AuthService {
  void start();
  String getNickByLoginPass(String login, String pass);
  Boolean changeNick(String from, String to);
  void stop();
}
