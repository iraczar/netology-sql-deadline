package ru.netology.qa.page;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Condition.visible;

public class DashboardPage {

    private final SelenideElement dashboard = $("[data-test-id='dashboard']");

    public DashboardPage checkDashboardIsVisible() {
        dashboard.shouldBe(visible);
        return this;
    }
}
