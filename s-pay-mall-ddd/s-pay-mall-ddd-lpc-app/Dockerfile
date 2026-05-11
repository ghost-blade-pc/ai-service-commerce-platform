# 1. 基础镜像：替换为 Java 17 的精简版镜像
FROM eclipse-temurin:17-jre-jammy

# 2. 作者：可以改成你自己的名字（看你 jar 包名带了 lpc，我就先写 lpc 啦）
# 注意：MAINTAINER 属于旧语法，虽然还能用，但 Docker 官方现在更推荐使用 LABEL
LABEL maintainer="lipeicheng"

# 3. 环境变量配置
ENV PARAMS=""
# 增加 JAVA_OPTS 的默认值，防止启动命令里该变量为空导致潜在问题
ENV JAVA_OPTS=""

# 4. 设置时区为中国（PRC）
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 5. 添加应用：把你 target 目录下的 jar 包复制到容器根目录
# 【请务必确认】：你通过 mvn clean package 打包出来的文件确实叫这个名字
ADD target/s-pay-mall-ddd-lpc-app.jar /s-pay-mall-ddd-lpc-app.jar

# 6. 启动命令
ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS /s-pay-mall-ddd-lpc-app.jar $PARAMS"]