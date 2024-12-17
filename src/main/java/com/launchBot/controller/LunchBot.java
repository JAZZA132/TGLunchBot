package com.launchBot.controller;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.toIntExact;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import com.launchBot.LaunchBotApplication;

@SuppressWarnings("TextBlockMigration")
public class LunchBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final LunchRotationManager rotationManager;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final long group_chat_id = -4525033144L;
//    private final long aaron_chat_id = 1016114104L;

    public LunchBot(String botToken) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.rotationManager = new LunchRotationManager();

        // 初始化定时推送
        initializeScheduledNotifications();
    }

    @Override
    public void consume(Update update) {
        // 處理文字訊息
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message_text = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();

            switch (message_text) {
                case "/start":
                    sendStartMessage(chat_id);
                    break;
                case "/today":
                    sendMessage(chat_id, "今天輪值組：" + rotationManager.getTodayGroup());
                    break;
                case "/done":
                    sendMessage(chat_id, rotationManager.moveToNextGroup());
                    break;
                case "/list":
                    sendMessage(chat_id, rotationManager.getList());
                    break;
                case "/reset":
                    rotationManager.resetRotation();
                    sendMessage(chat_id, "已重置回第一組");
                    break;
                case "/revert":
                    sendMessage(chat_id, rotationManager.revert());
                    break;
            }
        }
        // 處理回調查詢
        else if (update.hasCallbackQuery()) {
            String call_data = update.getCallbackQuery().getData();
            long message_id = update.getCallbackQuery().getMessage().getMessageId();
            long chat_id = update.getCallbackQuery().getMessage().getChatId();

            if (call_data.equals("update_msg_text")) {
                String answer = "Updated message text";
                EditMessageText new_message = EditMessageText.builder()
                        .chatId(chat_id)
                        .messageId(toIntExact(message_id))
                        .text(answer)
                        .build();
                try {
                    telegramClient.execute(new_message);
                } catch (TelegramApiException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    // 發送啟動訊息，包含內嵌鍵盤
    private void sendStartMessage(long chat_id) {
        SendMessage message = SendMessage.builder()
                .chatId(chat_id)
                .text("歡迎使用午餐輪值Bot " + LaunchBotApplication.version +"!\n" +
                        "每週二三四中午12:30會自動推送\n" +
                        "可用指令:\n" +
                        "/today - 顯示今天輪值組\n" +
                        "/done - 標記今天輪值結束,換下一組\n" +
                        "/reset - 重新從第一組開始\n" +
                        "/list - 查看所有組別\n" +
                        "/revert - 回復上一個")
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace(System.err);
        }
    }

    // 發送一般文字訊息
    private void sendMessage(long chat_id, String text) {
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

    // 午餐輪值管理類別
    private static class LunchRotationManager {
        private final List<List<String>> rotationGroups;
        private int currentGroupIndex;

        public LunchRotationManager() {
            rotationGroups = new ArrayList<>();

            // 初始化午餐輪值組別
            rotationGroups.add(List.of("Stanley", "Aaron Lu"));
            rotationGroups.add(List.of("Eileen", "Gary"));
            rotationGroups.add(List.of("Beni", "Gina"));
            rotationGroups.add(List.of("Jerome", "Aaron Wang"));
            rotationGroups.add(List.of("Cathy", "Tommy（Aaron Lu）"));
            rotationGroups.add(List.of("Kent（Aaron Wang）", "Didi（Jerome）"));

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
    }

    private void initializeScheduledNotifications() {
        scheduler.scheduleAtFixedRate(() -> {
            LocalDate today = LocalDate.now();

            // 只在周二、三、四推送
            if (isNotificationDay(today)) {

                // 今日人員
                String todayGroup = rotationManager.getTodayGroup();
                // 輪換到下一組
                String nextGroupMessage = rotationManager.moveToNextGroup();

                sendMessage(group_chat_id, "🍽️ 今日人員：" + todayGroup + "\n" + "🍽️ 下組人員: " + nextGroupMessage);

                System.out.println("Automatic rotation: " + nextGroupMessage);
            }
        }, getInitialDelay(), 24, TimeUnit.HOURS);
    }

    // 计算初始延迟
    private long getInitialDelay() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Taipei"));
        ZonedDateTime nextRun = now.withHour(12).withMinute(30).withSecond(0);

        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }

        return TimeUnit.MILLISECONDS.toSeconds(
                Duration.between(now, nextRun).toMillis()
        );
    }

    // 判断是否为推送日（周二、三、四）
    private boolean isNotificationDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.TUESDAY ||
                dayOfWeek == DayOfWeek.WEDNESDAY ||
                dayOfWeek == DayOfWeek.THURSDAY;
    }

    // 添加一个关闭方法，以便在应用程序关闭时正确停止调度器
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}

