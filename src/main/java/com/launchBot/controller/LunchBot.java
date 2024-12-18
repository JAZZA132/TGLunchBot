package com.launchBot.controller;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.launchBot.Service.LunchRotationManager.sendMessage;
import static com.launchBot.Service.LunchRotationManager.sendStartMessage;
import static java.lang.Math.toIntExact;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import com.launchBot.Service.LunchRotationManager;

@SuppressWarnings("TextBlockMigration")
public class LunchBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final LunchRotationManager rotationManager;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

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

    //排程

    private void initializeScheduledNotifications() {
        scheduler.scheduleAtFixedRate(() -> {
            LocalDate today = ZonedDateTime.now(ZoneId.of("Asia/Taipei")).toLocalDate();
            //todo 移除
            System.out.println(today+"today__");

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
        //todo 移除
        System.out.println(nextRun+"nextRun __");

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

