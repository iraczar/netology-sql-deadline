package ru.netology.qa.data;

import lombok.Value;

import java.security.SecureRandom;

public class DataHelper {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private DataHelper() {
    }

    @Value
    public static class UserInfo {
        String login;
        String password;
    }

    public static UserInfo generateUser() {
        return new UserInfo("user_" + randomString(8), randomString(10) + "A1!");
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