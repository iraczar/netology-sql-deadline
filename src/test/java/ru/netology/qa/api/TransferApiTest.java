package ru.netology.qa.api;

import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.netology.qa.db.SqlHelper;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Задача №2 (необязательная): тестирование перевода между картами через REST API.
 * Использует демо-пользователя vasya/qwerty123, которого сам SUT создаёт при старте
 * (проверено вручную: логин, код из auth_codes, verification, token — весь флоу реально
 * пройден curl-запросами против запущенного app-deadline.jar перед написанием этого теста).
 * <p>
 * Карты демо-пользователя vasya (тоже проверено вживую):
 * 5559 0000 0000 0001 и 5559 0000 0000 0002, баланс каждой изначально 10000 руб.
 * Поле amount в /api/transfer передаётся в РУБЛЯХ (баланс в БД хранится в копейках
 * и делится на 100 при отдаче через /api/cards) — это не баг, это конвертация единиц.
 */
class TransferApiTest {

    private static final String DEMO_LOGIN = "vasya";
    private static final String DEMO_PASSWORD = "qwerty123";
    private static final String CARD_1 = "5559 0000 0000 0001";
    private static final String CARD_2 = "5559 0000 0000 0002";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost:9999";
    }

    private String login() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"login\":\"" + DEMO_LOGIN + "\",\"password\":\"" + DEMO_PASSWORD + "\"}")
                .when()
                .post("/api/auth")
                .then()
                .statusCode(200);

        String code = SqlHelper.getVerificationCode(DEMO_LOGIN);

        return given()
                .contentType(ContentType.JSON)
                .body("{\"login\":\"" + DEMO_LOGIN + "\",\"code\":\"" + code + "\"}")
                .when()
                .post("/api/auth/verification")
                .then()
                .statusCode(200)
                .extract().path("token");
    }

    @Test
    @Description("Успешная авторизация возвращает JWT-токен, а список карт по нему — 2 карты демо-пользователя")
    void shouldReturnCardsAfterSuccessfulAuth() {
        String token = login();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/cards")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2));
    }

    @Test
    @Description("Перевод между двумя своими картами уменьшает баланс карты-источника " +
            "и увеличивает баланс карты-получателя на ту же сумму")
    void shouldTransferMoneyBetweenOwnCards() {
        String token = login();
        int amountRub = 10;

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"from\":\"" + CARD_1 + "\",\"to\":\"" + CARD_2 + "\",\"amount\":" + amountRub + "}")
                .when()
                .post("/api/transfer")
                .then()
                .statusCode(200);

        int balanceCard1 = SqlHelper.getCardBalanceInKopecks(CARD_1);
        int balanceCard2 = SqlHelper.getCardBalanceInKopecks(CARD_2);

        // изначально у каждой карты было 1 000 000 копеек (10000 руб.)
        assertEquals(1_000_000 - amountRub * 100, balanceCard1);
        assertEquals(1_000_000 + amountRub * 100, balanceCard2);
    }

    /**
     * Найденный дефект (воспроизведён вручную через curl перед написанием теста):
     * перевод на номер карты, которого не существует в БД, всё равно возвращает HTTP 200,
     * деньги списываются с карты-источника без какой-либо проверки существования получателя.
     * Ожидаемое поведение — 4xx с понятной ошибкой. Обязательно заведите issue (см. README.md).
     * Тест ниже фиксирует фактическое (ошибочное) поведение.
     */
    @Test
    @Description("Известный баг: перевод на несуществующий номер карты всё равно возвращает 200 " +
            "— см. README, раздел 'Найденные дефекты'")
    void knownBug_transferToNonExistingCardStillReturns200() {
        String token = login();

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"from\":\"" + CARD_1 + "\",\"to\":\"9999 9999 9999 9999\",\"amount\":1}")
                .when()
                .post("/api/transfer")
                .then()
                .statusCode(200); // по-хорошему здесь должна быть ошибка 4xx
    }
}
