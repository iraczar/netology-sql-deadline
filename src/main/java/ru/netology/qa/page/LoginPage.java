package ru.netology.qa.page;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

/**
 * Локаторы (data-test-id) проверены напрямую внутри app-deadline.jar:
 * static/static/js/main.324da39a.chunk.js содержит
 * "login", "password", "action-login", "code", "action-verify",
 * "dashboard", "error-notification".
 */
public class LoginPage {

    private final SelenideElement loginField = $("[data-test-id='login'] input");
    private final SelenideElement passwordField = $("[data-test-id='password'] input");
    private final SelenideElement loginButton = $("[data-test-id='action-login']");
    private final SelenideElement errorNotification = $("[data-test-id='error-notification']");

    public VerificationPage validLogin(String login, String password) {
        loginField.setValue(login);
        passwordField.setValue(password);
        loginButton.click();
        return new VerificationPage();
    }

    public void login(String login, String password) {
        loginField.setValue(login);
        passwordField.setValue(password);
        loginButton.click();
    }

    public SelenideElement getErrorNotification() {
        return errorNotification;
    }
}
