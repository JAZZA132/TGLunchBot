package com.launchBot.controller;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.launchBot.Service.LunchRotationManager.sendMessage;
import static com.launchBot.Service.LunchRotationManager.sendStartMessage;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import com.launchBot.Service.LunchRotationManager;

@SuppressWarnings("TextBlockMigration")
public class LunchBot implements LongPollingSingleThreadUpdateConsumer {
    private boolean isSchedulerEnabled = true;
    private final TelegramClient telegramClient;
    private final LunchRotationManager rotationManager;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static final long group_chat_id = -4525033144L;
    public static final long aaron_chat_id = 1016114104L;

    public LunchBot(String botToken) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.rotationManager = new LunchRotationManager();

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
                case "/scheduler_off":
                    if (isSchedulerEnabled) {
                        scheduler.shutdown();
                        isSchedulerEnabled = false;
                        sendMessage(chat_id, "排程已關閉 ⏸️");
                    } else {
                        sendMessage(chat_id, "排程已經是關閉狀態");
                    }
                    break;
                case "/scheduler_on":
                    if (!isSchedulerEnabled) {
                        scheduler.shutdownNow(); // 確保舊的排程完全關閉
                        this.scheduler = Executors.newScheduledThreadPool(1);
                        initializeScheduledNotifications();
                        isSchedulerEnabled = true;
                        sendMessage(chat_id, "排程已開啟 ▶️");
                    } else {
                        sendMessage(chat_id, "排程已經是開啟狀態");
                    }
                    break;
                case "/scheduler_status":
                    String status = isSchedulerEnabled ? "開啟 ▶️" : "關閉 ⏸️";
                    sendMessage(chat_id, "目前排程狀態: " + status);
                    break;
            }
        }
    }

    //排程
    private void initializeScheduledNotifications() {
        scheduler.scheduleAtFixedRate(() -> {

            ZonedDateTime executionTime = ZonedDateTime.now(ZoneId.of("Asia/Taipei"));
            System.out.println("Task executed at: " + executionTime);

            LocalDate today = executionTime.toLocalDate();

            // 只在周二、三、四,非國定假日推送
            if (isNotificationDay(today)) {

                // 今日人員
                String todayGroup = rotationManager.getTodayGroup();
                // 輪換到下一組
                String nextGroupMessage = rotationManager.moveToNextGroup();

                sendMessage(group_chat_id, "🍽️ 今日人員：" + todayGroup + "\n" + "🍽️ 下組人員: " + nextGroupMessage);

                System.out.println("Automatic rotation: " + nextGroupMessage);
            }
        }, getInitialDelay(), TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    }

    // 计算初始延迟
    private long getInitialDelay() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Taipei"));
        ZonedDateTime nextRun = now.withHour(12).withMinute(30).withSecond(0);

        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }


        long delay = Duration.between(now, nextRun).toMillis();

        System.out.println("延遲時間" + delay);

        return Math.max(delay, 0);
    }

    // 判断是否为推送日（周二、三、四）
    private boolean isNotificationDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        boolean isWorkday = dayOfWeek == DayOfWeek.TUESDAY ||
                dayOfWeek == DayOfWeek.WEDNESDAY ||
                dayOfWeek == DayOfWeek.THURSDAY;

        // 如果不是工作日，直接返回 false
        if (!isWorkday) {
            return false;
        }

        // 讀取假日檔案並檢查是否為假日
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config/2025Date.txt");
            if (inputStream == null) {
                throw new FileNotFoundException("File not found: config/2025Date.txt");
            }

            // 將 InputStream 轉為 List<String>
            List<String> holidays;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                holidays = reader.lines().toList();
            }

            // 將當前日期格式化為與檔案相同的格式 (yyyy/M/d)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
            String dateStr = date.format(formatter);

            // 如果日期在假日清單中，返回 false
            return !holidays.contains(dateStr);
        } catch (IOException e) {
            System.err.println("無法讀取假日檔案: " + e.getMessage());
            // 如果讀取檔案失敗，只依據週間判斷
            return true;
        }
    }
}

