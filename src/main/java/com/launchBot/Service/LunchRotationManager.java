package com.launchBot.Service;

import java.util.ArrayList;
import java.util.List;

import static com.launchBot.LaunchBotApplication.botToken;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import com.launchBot.LaunchBotApplication;

public class LunchRotationManager {
    private final List<List<String>> rotationGroups;
    private int currentGroupIndex;
    private static final TelegramClient telegramClient = new OkHttpTelegramClient(botToken);


    public LunchRotationManager() {
        rotationGroups = new ArrayList<>();

        // 初始化午餐輪值組別
        rotationGroups.add(List.of("Stanley", "Aaron Lu"));
        rotationGroups.add(List.of("Eileen", "Gary"));
        rotationGroups.add(List.of("Beni", "Gina"));
        rotationGroups.add(List.of("Jerome", "Aaron Wang"));
        rotationGroups.add(List.of("Cathy", "Tommy"));
        rotationGroups.add(List.of("Kent", "Didi（Jerome）"));

        currentGroupIndex = 0;
    }

    public String getTodayGroup() {
        if (rotationGroups.isEmpty()) {
            return "沒有設定午餐輪值組別";
        }
        return String.join(" 和 ", rotationGroups.get(currentGroupIndex));
    }

    public String getList() {
        if (rotationGroups.isEmpty()) {
            return "沒有設定午餐輪值組別";
        }
        StringBuilder stringBuffer = new StringBuilder();
        for (List<String> rotationGroup : rotationGroups) {
            stringBuffer.append(rotationGroup);
            stringBuffer.append("\n");
        }
        return stringBuffer.toString();
    }

    public String moveToNextGroup() {
        if (rotationGroups.isEmpty()) {
            return "沒有可輪值的組別";
        }

        currentGroupIndex = (currentGroupIndex + 1) % rotationGroups.size();
        return "已換至下一組: " + getTodayGroup();
    }

    public void resetRotation() {
        currentGroupIndex = 0;
    }

    public String revert() {
        if (rotationGroups.isEmpty()) {
            return "沒有可輪值的組別";
        }
        if (currentGroupIndex == 0) {
            currentGroupIndex = rotationGroups.size() - 1;
        } else {
            currentGroupIndex = currentGroupIndex - 1;
        }
        return "已回復至上一組: " + getTodayGroup();
    }

    public static void sendMessage(long chat_id, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chat_id)
                .text(text)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace(System.err);
        }
    }


    // 發送啟動訊息，包含內嵌鍵盤
    public static void sendStartMessage(long chat_id) {
        SendMessage message = SendMessage.builder()
                .chatId(chat_id)
                .text("歡迎使用午餐輪值Bot " + LaunchBotApplication.version + "!\n" +
                        "每週二三四中午12:30會自動推送\n" +
                        "遇國定假日或公司假日會暫停(但GameDay無法事先預知)" +
                        "可用指令:\n" +
                        "/today - 顯示今天輪值組\n" +
                        "/done - 標記今天輪值結束,換下一組\n" +
                        "/reset - 重新從第一組開始\n" +
                        "/list - 查看所有組別\n" +
                        "/revert - 回復上一個\n" +
                        "/scheduler_status - 排程狀態\n" +
                        "/scheduler_on - 開啟排程\n" +
                        "/scheduler_off - 關閉排程\n")
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace(System.err);
        }
    }
}
