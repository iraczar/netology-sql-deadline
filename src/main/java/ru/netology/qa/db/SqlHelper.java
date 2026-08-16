package ru.netology.qa.db;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Единственное место в проекте, которое напрямую обращается к базе данных.
 * Вся работа идёт через Apache Commons DBUtils (QueryRunner), как того требует задание.
 * <p>
 * Параметры подключения берутся из системных свойств (заданы в build.gradle,
 * значения по умолчанию совпадают с docker-compose.yml и со значениями,
 * зашитыми внутри app-deadline.jar по умолчанию):
 * db.url      -> jdbc:mysql://localhost:3306/app
 * db.user     -> app
 * db.password -> pass
 */
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

    /**
     * Создаёт нового пользователя напрямую в БД, минуя SUT.
     * Пароль сохраняется в зашифрованном (BCrypt) виде — так же,
     * как это делает сам SUT (внутри используется Spring Security PasswordEncoder),
     * поэтому созданный пользователь сможет залогиниться через форму с открытым паролем.
     *
     * @param login       логин пользователя
     * @param rawPassword пароль в открытом виде (именно его нужно вводить в форму)
     * @return id созданного пользователя
     */
    public static String createUser(String login, String rawPassword) {
        String id = UUID.randomUUID().toString();
        String hashedPassword = ENCODER.encode(rawPassword);
        try (Connection conn = getConnection()) {
            RUNNER.update(conn,
                    "INSERT INTO users(id, login, password, status) VALUES (?, ?, ?, ?);",
                    id, login, hashedPassword, "active");
            return id;
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось создать пользователя в БД", e);
        }
    }

    /**
     * Возвращает последний сгенерированный код подтверждения для пользователя.
     * Именно так тест "подсматривает" одноразовый код, который в реальной жизни
     * пользователь получил бы по SMS/email, и вводит его в форму верификации.
     */
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
                throw new RuntimeException("Не удалось получить код подтверждения из БД", e);
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
        throw new RuntimeException("Код подтверждения не появился в БД для пользователя " + login + " за 5 секунд");
    }

    /**
     * Возвращает текущий баланс карты в копейках напрямую из БД —
     * используется, чтобы проверить результат перевода независимо от ответа API.
     */
    public static int getCardBalanceInKopecks(String cardNumber) {
        try (Connection conn = getConnection()) {
            Object result = RUNNER.query(conn,
                    "SELECT balance_in_kopecks FROM cards WHERE number = ?;",
                    new ScalarHandler<>(), cardNumber);
            return ((Number) result).intValue();
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось получить баланс карты из БД", e);
        }
    }

    /**
     * Удаляет пользователя и всё, что на него ссылается (коды подтверждения, карты).
     * Используется в @AfterAll, чтобы не засорять базу тестовыми пользователями.
     */
    public static void cleanUpUser(String login) {
        try (Connection conn = getConnection()) {
            RUNNER.update(conn,
                    "DELETE FROM auth_codes WHERE user_id = (SELECT id FROM users WHERE login = ?);", login);
            RUNNER.update(conn,
                    "DELETE FROM cards WHERE user_id = (SELECT id FROM users WHERE login = ?);", login);
            RUNNER.update(conn, "DELETE FROM users WHERE login = ?;", login);
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось удалить тестового пользователя", e);
        }
    }
}
