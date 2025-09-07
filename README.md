# 🎱 Snooker Orakle – Backend & Frontend

This project provides a REST API and frontend dashboard for snooker player rankings, stats, and predictions.

---

## 🌍 Deployment URLs

### 🔹 Backend (Spring Boot API)

- **Production**:  
  [http://snookerorakle.eu-west-2.elasticbeanstalk.com/swagger-ui/index.html#/](http://snookerorakle.eu-west-2.elasticbeanstalk.com/swagger-ui/index.html#/)

- **Staging**:  
  [http://snookerrank-stg-restapp.eu-west-2.elasticbeanstalk.com/](http://snookerrank-stg-restapp.eu-west-2.elasticbeanstalk.com/)

### 🔹 Frontend (Vercel Dashboard)

- [https://snooker-sand.vercel.app/dashboard](https://snooker-sand.vercel.app/dashboard)

---

## 🛠️ Backend Setup

### ✅ Prerequisites

- Java 21+
- Maven 3.6+
- Microsoft SQL Server (local or AWS RDS)
- Spring Boot 6

---

### 🚧 Build Instructions

```bash
mvn clean install

mvn spring-boot:run

cd /target

java -jar snookerrank-2.4.1.jar --spring.profiles.active=stg