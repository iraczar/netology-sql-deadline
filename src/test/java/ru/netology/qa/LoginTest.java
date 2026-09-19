package ru.netology.qa;

import io.qameta.allure.Description;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.netology.qa.data.DataHelper;
import ru.netology.qa.db.SqlHelper;
import ru.netology.qa.page.DashboardPage;
import ru.netology.qa.page.LoginPage;
import ru.netology.qa.page.VerificationPage;

import static com.codeborne.selenide.Selenide.open;

class LoginTest extends BaseTest {

    private static final String AUTH_INVALID_MESSAGE = "Ошибка! Неверно указан логин или пароль";

    private DataHelper.UserInfo user;

    @BeforeEach
    void createTestUser() {
        user = DataHelper.generateUser();
        SqlHelper.createUser(user.getLogin(), user.getPassword());
    }

    @AfterEach
    void removeTestUser() {
        SqlHelper.cleanUpUser(user.getLogin());
    }

    @Test
    @Description("Успешный вход с валидным кодом из БД открывает личный кабинет")
    void shouldLoginWithValidCredentialsAndVerificationCode() {
        open("/");
        LoginPage loginPage = new LoginPage();
        VerificationPage verificationPage = loginPage.validLogin(user.getLogin(), user.getPassword());

        String code = SqlHelper.getVerificationCode(user.getLogin());
        DashboardPage dashboardPage = verificationPage.validVerify(code);

        dashboardPage.checkDashboardIsVisible();
    }

    @Test
    @Description("Неверный пароль показывает сообщение об ошибке")
    void shouldShowErrorMessageOnWrongPassword() {
        open("/");
        LoginPage loginPage = new LoginPage();
        loginPage.login(user.getLogin(), DataHelper.getInvalidPassword());

        loginPage.checkErrorNotification(AUTH_INVALID_MESSAGE);
    }

    @Test
    @Description("Известный баг: блокировка после 3 неверных попыток не срабатывает")
    void knownBug_userIsNotBlockedAfterThreeWrongAttempts() {
        open("/");
        LoginPage loginPage = new LoginPage();

        for (int attempt = 1; attempt <= 3; attempt++) {
            loginPage.login(user.getLogin(), DataHelper.getInvalidPassword());
            loginPage.checkErrorNotification(AUTH_INVALID_MESSAGE);
            open("/");
            loginPage = new LoginPage();
        }

        VerificationPage verificationPage = loginPage.validLogin(user.getLogin(), user.getPassword());
        String code = SqlHelper.getVerificationCode(user.getLogin());
        DashboardPage dashboardPage = verificationPage.validVerify(code);

        dashboardPage.checkDashboardIsVisible();
    }
}