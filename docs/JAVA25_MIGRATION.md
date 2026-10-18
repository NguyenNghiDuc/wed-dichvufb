# GrowLab – đồng bộ lên Java 25 (18/10/2026)

Mục tiêu: khởi động ứng dụng GrowLab trên SDKMAN JDK 25 trong Codespaces, khắc phục `RequireJavaVersion [17,18)`, đồng bộ CI và Docker. Đây là thay đổi về nền tảng chạy/build; **không đổi business logic, Flyway migrations hay dữ liệu sản xuất**.

## Tập hợp phiên bản

| Thành phần | Trước | Sau |
| --- | --- | --- |
| Java build / runtime | 17 | **25** |
| Spring Boot parent | 3.2.5 | **3.5.16** |
| Lombok (managed override) | Boot-managed cũ | **1.18.42** |
| JaCoCo | 0.8.12 | **0.8.15** |
| Maven Enforcer | 3.5.0 | **3.6.3** |
| Maven Enforcer Java range | `[17,18)` | `[25,26)` |
| Docker build | maven:3.9.9-eclipse-temurin-17 | **maven:3.9.11-eclipse-temurin-25** |
| Docker runtime | eclipse-temurin:17-jre-alpine | **eclipse-temurin:25-jre-alpine** |
| GitHub Actions Java CI / CodeQL | 17 | **25** |

Spring Boot 3.5.16 được tài liệu Spring xác nhận tương thích Java 25: https://docs.spring.io/spring-boot/3.5/system-requirements.html

JaCoCo từ 0.8.14 chính thức hỗ trợ bytecode Java 25, còn Lombok từ 1.18.40 hỗ trợ JDK 25.

## Codespaces – bỏ ép Java 17 cũ

1. Mở terminal trong repo, kiểm tra `java -version` và `mvn -version`.
2. Nếu trước đó đã thêm `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` vào `~/.bashrc` hoặc `~/.profile`, hãy bỏ các dòng đó (và PATH tương ứng).
3. Dùng SDKMAN: `sdk list java | grep '25.0.4'` để xác nhận phiên bản đã cài, sau đó `sdk use java 25.0.4+1-ms` khi đúng phiên bản đó có mặt. Nếu SDKMAN đang chọn JDK 25 rồi, không cần đổi.
4. `mvn -B --no-transfer-progress clean verify`; `mvn spring-boot:run`.
5. Nếu `JAVA_HOME` vẫn trỏ đến 17, sửa shell config và mở terminal mới; **không bỏ Maven Enforcer**.

## Gate trước merge

- Java CI: chạy PostgreSQL 16/Flyway, unit/integration, Jacoco report, JAR smoke và Docker image.
- CodeQL phân tích compile Maven trên Java 25.
- Kiểm thử hành vi Spring Boot 3.5.16, nhất là Spring Security/CSRF, Thymeleaf, Spring Data JPA, Flyway.
- UAT trên Codespaces: /, /login, /register, dịch vụ, /campaigns; không đưa production trực tiếp.
- Không merge khi exact-head CI failed, queued hoặc các test trên không đạt.
- Điều phối với W9 (#46) vì hai workflow cùng được chỉnh trong PR Java25, W10 (#47) vì Dockerfile được đổi.

Lưu ý: 3.5.16 là bản cuối OSS của 3.5.x theo thông báo tháng 6/2026; kế hoạch nâng Spring Boot 4.x là công việc tương lai riêng, cần kiểm thử tương thích.
