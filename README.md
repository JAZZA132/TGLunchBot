參考資料
https://rubenlagus.github.io/TelegramBotsDocumentation/getting-started.html#send-messages

打包成jar
```
mvn clean package
```

進到專案裡面 <br>
使用docker打包成image <br>
打包指令('.' 不可省略) <br>
```
docker build -t lunchbot .
```

镜像打上 Docker Hub 標籤 <br>
```
docker tag lunchbot jazza132/lunchbot:latest
```

sudo docker login -u j5224821120@yahoo.com.tw
docker push jazza132/lunchbot:latest
```

前往gcp <br>
部屬docker image <br>

```
sudo docker login -u ${yourEmail}
sudo docker pull jazza132/lunchbot
sudo docker run -d -p 8080:8080 lunchbot
```

~~docker run -p 8080:8080 lunchbot jazza132/lunchbot~~