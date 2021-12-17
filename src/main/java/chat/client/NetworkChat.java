package chat.client;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;


public class NetworkChat extends JFrame {

  private JTextField message;
  private JTextArea chatArea;
  private JTextArea clientsList;
  private Socket socket;
  private DataInputStream in;
  private DataOutputStream out;
  private String nick = "";
  private String login = "";

  public NetworkChat() {
    setTitle("Сетевой чат");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setBounds(300, 300, 400, 400);
    JLabel header = new JLabel("Сетевой чат");
    header.setFont(new Font("Arial", Font.BOLD, 16));
    add(header, BorderLayout.PAGE_START);

    chatArea = new JTextArea();
    chatArea.setEditable(false);
    add(chatArea, BorderLayout.CENTER);

    clientsList = new JTextArea();
    clientsList.setEditable(false);
    add(clientsList, BorderLayout.LINE_END);

    JPanel downPanel = new JPanel();
    downPanel.setLayout(new BoxLayout(downPanel, BoxLayout.LINE_AXIS));
    message = new JTextField();
    message.addActionListener(e -> {
      sendMessage();
    });
    message.setText("/auth login1 pass1");
    downPanel.add(message);
    JButton sendButton = new JButton("Отправить");
    sendButton.addActionListener(e -> {
      sendMessage();
    });
    downPanel.add(sendButton);

    add(downPanel, BorderLayout.PAGE_END);
    setVisible(true);
  }

  private void closeConnection() {
    chatArea.append("* Вы вышли из чата\n");
    message.setText("/auth login1 pass1");
    clientsList.setText("");
    try {
      in.close();
      out.close();
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    new NetworkChat();
  }

  public void sendMessage() {
    String trimmedMessage = this.message.getText().trim();
    if (!trimmedMessage.isEmpty()) {
      message.setText("");
      try {
        if (socket == null || socket.isClosed()) {
          socket = new Socket("localhost", 8189);
          socket.setSoTimeout(120000);
          in = new DataInputStream(socket.getInputStream());
          out = new DataOutputStream(socket.getOutputStream());
          new Thread(() -> {
            try {
              while (true) {
                String strFromServer = in.readUTF();
                if (strFromServer.startsWith("/authok")) {
                  nick = strFromServer.split("\\s+")[1];
                  chatArea.append("* Вы авторизованы как: " + nick + "\n");
                  socket.setSoTimeout(0);
                  break;
                }
                chatArea.append(strFromServer + "\n");
              }
              while (true) {
                String strFromServer = in.readUTF();
                if (strFromServer.equalsIgnoreCase("/end")) {
                  break;
                }
                if (strFromServer.startsWith("/clients")) {
                  String clients = strFromServer.substring("/clients ".length());
                  clientsList.setText(clients.replace(' ', '\n'));
                } else {
                  chatArea.append(strFromServer + "\n");
                }
              }
            } catch (Exception e) {
              e.printStackTrace();
            } finally {
              closeConnection();
            }
          }).start();
        }
        out.writeUTF(trimmedMessage);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
