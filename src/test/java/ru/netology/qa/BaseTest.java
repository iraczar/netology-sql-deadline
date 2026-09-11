package ru.netology.qa;

import com.codeborne.selenide.Configuration;
import org.junit.jupiter.api.BeforeAll;

public class BaseTest {

    @BeforeAll
    static void setUpAll() {
        Configuration.baseUrl = "http://localhost:9999";
        Configuration.browser = "chrome";
        Configuration.headless = true;
        Configuration.timeout = 10000;
        Configuration.browserSize = "1920x1080";
        Configuration.holdBrowserOpen = false;
    }
}