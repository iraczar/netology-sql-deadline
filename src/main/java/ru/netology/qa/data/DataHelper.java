package ru.netology.qa.data;

import java.security.SecureRandom;

/**
 * Класс отвечает только за генерацию тестовых данных.
 * Никакой логики работы с БД или UI здесь нет — это специально:
 * DataHelper генерирует данные, SqlHelper кладёт их в базу,
 * Page Object использует их в браузере.
 */
public class DataHelper {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private DataHelper() {
    }

    /**
     * DTO с данными пользователя, который ещё не существует в БД.
     * password хранится в открытом виде — именно его мы будем вводить в форму логина.
     * SqlHelper при сохранении в БД зашифрует его через BCrypt.
     */
    public static class UserInfo {
        private final String login;
        private final String password;

        public UserInfo(String login, String password) {
            this.login = login;
            this.password = password;
        }

        public String getLogin() {
            return login;
        }

        public String getPassword() {
            return password;
        }
    }

    public static UserInfo generateUser() {
        String login = "user_" + randomString(8);
        String password = randomString(10) + "A1!";
        return new UserInfo(login, password);
    }

    public static String getInvalidPassword() {
        return "invalid_" + randomString(6);
    }

    private static String randomString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
