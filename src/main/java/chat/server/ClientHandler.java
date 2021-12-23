package chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ClientHandler {
  public static ExecutorService clientPool = Executors.newCachedThreadPool();

  private MyServer myServer;
  private Socket socket;
  private DataInputStream in;
  private DataOutputStream out;

  private String name;

  public String getName() {
    return name;
  }

  public ClientHandler(MyServer myServer, Socket socket) {
    try {
      this.myServer = myServer;
      this.socket = socket;
      this.in = new DataInputStream(socket.getInputStream());
      this.out = new DataOutputStream(socket.getOutputStream());
      this.name = "";
      clientPool.execute(() -> {
        try {
          authentication();
          readMessages();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          closeConnection();
        }
      });
    } catch (IOException e) {
      throw new RuntimeException("Проблемы при создании обработчика клиента");
    }
  }

  public void authentication() throws IOException {
    while (true) {
      String str = in.readUTF();
      if (str.startsWith("/auth")) {
        String[] parts = str.split("\\s");
        String nick = myServer.getAuthService().getNickByLoginPass(parts[1], parts[2]);
        if (nick != null) {
          if (!myServer.isNickBusy(nick)) {
            sendMsg("/authok " + nick + " " + parts[1]);
            name = nick;
            myServer.broadcastMsg(name + " зашел в чат");
            myServer.subscribe(this);
            return;
          } else {
            sendMsg("Учетная запись уже используется");
          }
        } else {
          sendMsg("Неверные логин/пароль");
        }
      }
    }
  }

  public void readMessages() throws IOException {
    while (true) {
      String strFromClient = in.readUTF();
      System.out.println("от " + name + ": " + strFromClient);
      if (strFromClient.equals("/end")) {
        out.writeUTF(strFromClient);
        return;
      }
      if (strFromClient.startsWith("/w")) {
        String[] parts = strFromClient.split("\\s");
        String to = parts[1];
        String message = strFromClient.substring(4 + to.length());
        if (myServer.unicastMsg(to, "От " + getName() + ": " + message)) {
          out.writeUTF("Личное сообщение для " + to + ": " + message);
        } else {
          out.writeUTF("* Участника с ником " + to + " нет в чат-комнате");
        }
      } else if (strFromClient.startsWith("/nick")) {
          String[] parts = strFromClient.split("\\s");
          if (parts.length != 2) {
            out.writeUTF("Неверный формат ввода, попоробуйте /nick новый ник");
            continue;
          }
          String nickTo = parts[1];
          if (myServer.getAuthService().changeNick(name, nickTo)) {
            myServer.broadcastMsg(this.name + "теперь известен как " + nickTo);
            this.name = nickTo;
            myServer.broadcastClientsList();
          } else {
            out.writeUTF("Такой ник уже существует.");
          }
        } else {
          myServer.broadcastMsg(name + ": " + strFromClient);
        }
      }
    }

  public void sendMsg(String msg) {
    try {
      out.writeUTF(msg);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void closeConnection() {
    myServer.unsubscribe(this);
    myServer.broadcastMsg(name + " вышел из чата");
    try {
      in.close();
      out.close();
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}