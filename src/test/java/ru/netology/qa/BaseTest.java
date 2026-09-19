package ru.netology.qa;

import com.codeborne.selenide.Configuration;
import org.junit.jupiter.api.BeforeAll;

public class BaseTest {

    @BeforeAll
    static void setUpAll() {
        Configuration.baseUrl = "http://localhost:9999";
    }
}