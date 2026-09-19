package ru.netology.qa.page;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

public class DashboardPage {

    private final SelenideElement dashboard = $("[data-test-id='dashboard']");

    public void checkDashboardIsVisible() {
        dashboard.shouldBe(visible).shouldHave(text("Личный кабинет"));
    }
}