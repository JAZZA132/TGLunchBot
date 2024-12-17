package com.launchBot;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import com.launchBot.controller.LunchBot;

@SpringBootApplication
public class LaunchBotApplication {
    public static final String version = "2.2";

    public static void main(String[] args) {
        String botToken = "YourKey";
        try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
            botsApplication.registerBot(botToken, new LunchBot(botToken));
            System.out.println("LCSLunchBot " + version + " successfully started!");
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
