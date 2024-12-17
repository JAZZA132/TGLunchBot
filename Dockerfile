# 使用官方的 OpenJDK 基底映像檔
FROM openjdk:21-jdk

# 設定工作目錄
WORKDIR /app

# 複製 Spring Boot JAR 檔案到容器
COPY target/launchBot-0.0.1-SNAPSHOT.jar app.jar


# # 指定容器啟動時執行的命令
# ENTRYPOINT ["java", "-jar", "app.jar"]
# # 開放應用程式使用的 Port
EXPOSE 8080

# 設定環境變量,配合render
# ENV SERVER_ADDRESS=0.0.0.0
# ENV SERVER_PORT=10000

ENTRYPOINT ["java", "-jar", "app.jar"]
# CMD ["--server.address=${SERVER_IP}", "--server.port=${SERVER_PORT}"]
