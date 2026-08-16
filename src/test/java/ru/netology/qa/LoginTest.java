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
    @Description("Пользователь, созданный напрямую в БД через SqlHelper, " +
            "должен успешно пройти двухфакторный вход: логин/пароль -> код из БД -> дашборд")
    void shouldLoginWithValidCredentialsAndVerificationCode() {
        open("/");
        LoginPage loginPage = new LoginPage();
        VerificationPage verificationPage = loginPage.validLogin(user.getLogin(), user.getPassword());

        String code = SqlHelper.getVerificationCode(user.getLogin());
        DashboardPage dashboardPage = verificationPage.validVerify(code);

        dashboardPage.checkDashboardIsVisible();
    }

    @Test
    @Description("При неверном пароле форма логина должна показать сообщение об ошибке " +
            "и не пускать пользователя дальше")
    void shouldShowErrorMessageOnWrongPassword() {
        open("/");
        LoginPage loginPage = new LoginPage();
        loginPage.login(user.getLogin(), DataHelper.getInvalidPassword());

        loginPage.getErrorNotification().shouldBe(visible);
    }

    /**
     * Известный дефект SUT (воспроизведён вручную curl'ом): блокировка после 3 неверных
     * попыток не срабатывает. Тест фиксирует фактическое поведение — см. README,
     * раздел "Найденные дефекты".
     */
    @Test
    @Description("Известный баг: после 3 неверных попыток входа блокировка НЕ срабатывает " +
            "(ожидается по ТЗ, но не реализовано в SUT) — см. README, раздел 'Найденные дефекты'")
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