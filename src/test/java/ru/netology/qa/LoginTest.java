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

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;

class LoginTest extends BaseTest {

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
    @Description("Login s validnym kodom iz BD dolzhen otkryvat dashboard")
    void shouldLoginWithValidCredentialsAndVerificationCode() {
        open("/");
        LoginPage loginPage = new LoginPage();
        VerificationPage verificationPage = loginPage.validLogin(user.getLogin(), user.getPassword());

        String code = SqlHelper.getVerificationCode(user.getLogin());
        DashboardPage dashboardPage = verificationPage.validVerify(code);

        dashboardPage.checkDashboardIsVisible();
    }

    @Test
    @Description("Nevernyy parol dolzhen pokazyvat oshibku")
    void shouldShowErrorMessageOnWrongPassword() {
        open("/");
        LoginPage loginPage = new LoginPage();
        loginPage.login(user.getLogin(), DataHelper.getInvalidPassword());

        loginPage.getErrorNotification().shouldBe(visible);
    }

    @Test
    @Description("Izvestnyy bag: blokirovka posle 3 nevernyh popytok ne rabotaet")
    void knownBug_userIsNotBlockedAfterThreeWrongAttempts() {
        open("/");
        LoginPage loginPage = new LoginPage();

        for (int attempt = 1; attempt <= 3; attempt++) {
            loginPage.login(user.getLogin(), DataHelper.getInvalidPassword());
            loginPage.getErrorNotification().shouldBe(visible);
            open("/");
            loginPage = new LoginPage();
        }

        VerificationPage verificationPage = loginPage.validLogin(user.getLogin(), user.getPassword());
        String code = SqlHelper.getVerificationCode(user.getLogin());
        DashboardPage dashboardPage = verificationPage.validVerify(code);

        dashboardPage.checkDashboardIsVisible();
    }
}