package ru.netology.qa.db;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

public class SqlHelper {

    private static final QueryRunner RUNNER = new QueryRunner();
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private SqlHelper() {
    }

    private static Connection getConnection() throws SQLException {
        String url = System.getProperty("db.url", "jdbc:mysql://localhost:3306/app");
        String user = System.getProperty("db.user", "app");
        String password = System.getProperty("db.password", "pass");
        return DriverManager.getConnection(url, user, password);
    }

    public static String createUser(String login, String rawPassword) {
        String id = UUID.randomUUID().toString();
        String hashedPassword = ENCODER.encode(rawPassword);
        try (Connection conn = getConnection()) {
            RUNNER.update(conn,
                    "INSERT INTO users(id, login, password, status) VALUES (?, ?, ?, ?);",
                    id, login, hashedPassword, "active");
            return id;
        } catch (SQLException e) {
            throw new RuntimeException("Ne udalos sozdat polzovatelya v BD", e);
        }
    }

    public static String getVerificationCode(String login) {
        long deadline = System.currentTimeMillis() + 5000;
        String code = null;
        while (System.currentTimeMillis() < deadline) {
            try (Connection conn = getConnection()) {
                code = RUNNER.query(conn,
                        "SELECT ac.code FROM auth_codes ac " +
                                "JOIN users u ON u.id = ac.user_id " +
                                "WHERE u.login = ? " +
                                "ORDER BY ac.created DESC LIMIT 1;",
                        new ScalarHandler<String>(), login);
            } catch (SQLException e) {
                throw new RuntimeException("Ne udalos poluchit kod podtverzhdeniya iz BD", e);
            }
            if (code != null) {
                return code;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        throw new RuntimeException("Kod podtverzhdeniya ne poyavilsya v BD dlya " + login);
    }

    public static void cleanUpUser(String login) {
        try (Connection conn = getConnection()) {
            RUNNER.update(conn,
                    "DELETE FROM auth_codes WHERE user_id = (SELECT id FROM users WHERE login = ?);", login);
            RUNNER.update(conn,
                    "DELETE FROM cards WHERE user_id = (SELECT id FROM users WHERE login = ?);", login);
            RUNNER.update(conn, "DELETE FROM users WHERE login = ?;", login);
        } catch (SQLException e) {
            throw new RuntimeException("Ne udalos udalit testovogo polzovatelya", e);
        }
    }
}