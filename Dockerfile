# ============================================================
# 职业能力大数据服务平台 — Spring Boot Docker 镜像
# 构建：docker build -t occupation-app .
#
# 注意：镜像在容器构建阶段自动 Maven 打包，GitHub 同步到服务器后
# 不再依赖本机已经存在的 target/*.jar。
# ============================================================

FROM maven:3.9-eclipse-temurin-11 AS builder

WORKDIR /build
COPY pom.xml .
COPY occupation-common ./occupation-common
COPY occupation-auth ./occupation-auth
COPY occupation-analysis ./occupation-analysis
COPY occupation-report ./occupation-report
COPY occupation-recommend ./occupation-recommend
COPY occupation-api ./occupation-api
COPY occupation-crawler ./occupation-crawler
COPY occupation-web ./occupation-web
RUN mvn -pl occupation-web -am package -DskipTests

FROM eclipse-temurin:11-jre

WORKDIR /app

COPY --from=builder /build/occupation-web/target/occupation-web-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
