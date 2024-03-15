# MathHelper

GeeksForLess Technical Test for Java Basic Course.

It is a demo application made without any frameworks. It uses only JDBC to access PostgreSQL database. Slf4 Logging and JUnit tests are not counted.

## Installation
1. Install JDK and Maven.
2. Install PostgreSQL RDBMS.
3. Create "geeksmath" DB (and a new user if you do not want to use "postgres" user) for this application using the following commands:
``` sql
psql -U postgres
CREATE DATABASE geeksmath;
```
Check DB credentials in the **db.properties** file.

## Build and run
See **$build_and_run_for_windows.bat** for Windows.

## Used sources
Shunting yard algorithm: https://en.wikipedia.org/wiki/Shunting_yard_algorithm
Google :-)
