package chat.server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BaseAuthService implements AuthService {
    public static Connection connection;
    public static Statement stmt;
    private List<Entry> entries;

    private static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return connection;
    }

    private class Entry {
        private int id;
        private String login;
        private String pass;
        private String nick;

        public Entry(int id, String login, String pass, String nick) {
            this.id = id;
            this.login = login;
            this.pass = pass;
            this.nick = nick;
        }
    }

    @Override
    public void start() {
        connect();
        initAndFillTable();
        System.out.println("Сервис аутентификации запущен");
    }

    @Override
    public void stop() {
        System.out.println("Сервис аутентификации остановлен");
        disconnect();
    }

    private void initAndFillTable() {
        try {
            createLoginsTable();
            fillLoginsTable();
            this.entries = getLogins();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createLoginsTable() throws SQLException {
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS logins ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "login TEXT, " +
                "pass TEXT, " +
                "nick TEXT);");
    }

    private static void fillLoginsTable() throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM logins;");
        int countLogins = rs.getInt("count");
        if (countLogins == 0) {
            for (int i = 1; i <= 3; i++) {
                insertLogin(i);
            }
        }
    }

    private static void insertLogin(int number) throws SQLException {
        stmt.executeUpdate(String.format("INSERT INTO logins (login, pass, nick) VALUES ('login%d', 'pass%d', 'nick%d');", number, number, number));

    }

    private List<Entry> getLogins() throws SQLException {
        List<Entry> logins = new ArrayList<>(3);
        try (ResultSet rs = stmt.executeQuery("SELECT id, login, pass, nick FROM logins")) {
            while (rs.next()) {
                logins.add(new Entry(
                        rs.getInt("id"),
                        rs.getString("login"),
                        rs.getString("pass"),
                        rs.getString("nick")));
            }
        }
        return logins;
    }


    public BaseAuthService() {
        entries = new ArrayList<>();
        // entries.add(new Entry("login1", "pass1", "nick1"));1
        // entries.add(new Entry("login2", "pass2", "nick2"));
        // entries.add(new Entry("login3", "pass3", "nick3"));
    }

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:S:/chat_project/chatproject.db");
            connection.setAutoCommit(true);
            stmt = connection.createStatement();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            if (stmt != null) {
                stmt.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNickByLoginPass(String login, String pass) {
        for (Entry o : entries) {
            if (o.login.equals(login) && o.pass.equals(pass)) return o.nick;
        }
        return null;
    }

    @Override
    public Boolean changeNick(String from, String to) {
        Integer nickId = getIdByNick(to);
        if (nickId > 0) {
            return false;
        } else {
            PreparedStatement ps;
            try {
                ps = getConnection().prepareStatement("UPDATE logins SET nick = ? WHERE nick =?");
                ps.setString(1, to);
                ps.setString(2, from);
                ps.executeUpdate();
                for (Entry entry : entries) {
                    if (entry.nick.equals(from)) {
                        entry.nick = to;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    public Integer getIdByNick(String nick) {
        PreparedStatement ps;
        try {
            ps = getConnection().prepareStatement("SELECT id FROM logins WHERE nick = ?");
            ps.setString(1, nick);
            ResultSet rs = ps.executeQuery();
            if (rs.isClosed()) {
                return -1;
            } else {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}