# deploy-lab

A simple Spring Boot notes API for hands-on DevOps practice. Use this source to learn how to build, test, containerize, and deploy a backend service.

**Stack:** Java 24, Spring Boot 4.1.2, Maven Wrapper, Spring Data JPA, MySQL, Bean Validation, and Actuator. No frontend or Redis is required.

## Prerequisites

- JDK 24, matching `pom.xml`.
- MySQL 8 or later, with an administrator account to initialize the database.
- Internet access for the first Maven build to download dependencies.

Run commands from the project root unless stated otherwise.

## 1. Select Java 24

Windows PowerShell:

```powershell
Set-Location F:\DevOps\deploy-lab
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-24'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
.\mvnw.cmd -version
```

Adjust the paths to match your machine. These settings apply to the current terminal session.

On Linux or macOS, configure `JAVA_HOME` and `PATH` for your JDK 24 installation, then run:

```bash
java -version
sh mvnw -version
```

## 2. Initialize MySQL

Open [database/init.sql](database/init.sql), replace the sample password `change-me-local-only`, and execute it as a MySQL administrator using MySQL Workbench.

Alternatively, open the MySQL CLI:

```powershell
mysql -u root -p
```

Then run this command inside the MySQL client, adjusting the path as needed:

```sql
SOURCE F:/DevOps/deploy-lab/database/init.sql;
```

The script creates the `deploy_lab` database with `utf8mb4` encoding and a local `deploy_lab` user with permissions limited to that database. If the user already exists, its password is preserved; use the existing password or change it explicitly with `ALTER USER`.

Hibernate creates the `notes` table on the first application startup.

## 3. Configure the application

Copy [.env.example](.env.example) to `.env` if it does not already exist.

PowerShell:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Linux or macOS:

```bash
if [ ! -f .env ]; then cp .env.example .env; fi
```

Edit `.env` to match your database credentials:

```properties
DB_URL=jdbc:mysql://localhost:3306/deploy_lab?connectionTimeZone=UTC
DB_USERNAME=deploy_lab
DB_PASSWORD=replace-with-your-mysql-password
SERVER_PORT=8080
```

The application explicitly imports this file using:

```properties
spring.config.import=file:./.env[.properties]
```

**The `.env` file is required by the current configuration.** Its path is relative to the working directory. Use Java properties syntax: plain `KEY=value` entries without shell-style `export` statements or surrounding quotes. The file is already ignored by Git.

| Variable | Default if omitted | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/deploy_lab?connectionTimeZone=UTC` | MySQL JDBC URL |
| `DB_USERNAME` | `deploy_lab` | Database user |
| `DB_PASSWORD` | None; required | Database password |
| `SERVER_PORT` | `8080` | HTTP port |

Environment variables can override values from `.env`, but the file is still required by the current import configuration.

## 4. Run the application

PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux or macOS:

```bash
sh mvnw spring-boot:run
```

The default base URL is `http://localhost:8080`. Stop the application with `Ctrl+C`.

## 5. Try the API

| Method | Endpoint | Successful response |
| --- | --- | --- |
| `POST` | `/api/notes` | `201 Created`, the new note, and a `Location` header |
| `GET` | `/api/notes` | `200 OK`, notes ordered by descending ID; `[]` when empty |
| `GET` | `/api/notes/{id}` | `200 OK`, the requested note |
| `GET` | `/actuator/health` | `200 OK` with `{"status":"UP"}` when the application and database are healthy |

### PowerShell example

Open a second terminal:

```powershell
$base = 'http://localhost:8080'
Invoke-RestMethod "$base/actuator/health"

$body = @{
    title = 'Learn DevOps'
    content = 'Run Spring Boot with MySQL'
} | ConvertTo-Json

$note = Invoke-RestMethod "$base/api/notes" -Method Post -ContentType 'application/json; charset=utf-8' -Body $body
$note
Invoke-RestMethod "$base/api/notes"
Invoke-RestMethod "$base/api/notes/$($note.id)"
```

### curl example (Linux or macOS)

```bash
curl -i http://localhost:8080/actuator/health

curl -i -X POST http://localhost:8080/api/notes \
  -H 'Content-Type: application/json' \
  -d '{"title":"Learn DevOps","content":"Run Spring Boot with MySQL"}'

curl -i http://localhost:8080/api/notes

# Replace 1 with an ID returned by the API.
curl -i http://localhost:8080/api/notes/1
```

### Note format and validation

Example response; the ID and timestamp are illustrative:

```json
{
  "id": 1,
  "title": "Learn DevOps",
  "content": "Run Spring Boot with MySQL",
  "createdAt": "2026-10-26T12:00:00Z"
}
```

- `title` is required and must contain at most 200 characters.
- `content` is required and must contain at most 5000 characters.
- Blank values are rejected. Leading and trailing whitespace is stripped before saving.
- `id` is generated by the database; `createdAt` is generated by the application and returned in UTC.

### Error responses

- `400 Bad Request`: missing or invalid fields, malformed JSON, or an invalid ID.
- `404 Not Found`: the requested note does not exist.

Request errors use JSON Problem Detail. Field validation errors include an `errors` object:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/notes",
  "errors": {
    "title": "Title is required",
    "content": "Content is required"
  }
}
```

### Health check

Health includes database connectivity. If the application remains running but loses its database connection, the endpoint can return `503 Service Unavailable` with `{"status":"DOWN"}`.

Only the `health` actuator endpoint is exposed, and health details are hidden. See the [Spring Boot Actuator documentation](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html) for configuration details.

## 6. Test and build

With Java 24 selected and `.env` present:

```powershell
.\mvnw.cmd clean verify
```

Linux or macOS:

```bash
sh mvnw clean verify
```

Tests load the Spring application context and exercise the API through MockMvc. They use an in-memory H2 database through the `test` profile, so a running MySQL server is not required. The `.env` file is still required because the configuration import also applies during tests.

Tests cover creation, persistence, retrieval, ordering, empty results, validation, invalid IDs, malformed requests, missing notes, and actuator exposure. H2 is a test-only dependency and is not packaged in the application JAR. These tests do not replace verification against real MySQL.

The build produces `target/deploy-lab-0.0.1-SNAPSHOT.jar`.

Run it from the project root with MySQL running and `.env` configured:

```powershell
java -jar .\target\deploy-lab-0.0.1-SNAPSHOT.jar
```

Linux or macOS:

```bash
java -jar target/deploy-lab-0.0.1-SNAPSHOT.jar
```

Stop any previous application instance first to avoid a port conflict.

## Project structure

```text
deploy-lab/
  .env.example
  .mvn/wrapper/
  database/init.sql
  mvnw
  mvnw.cmd
  pom.xml
  src/main/java/com/deploy_lab/
    DeployLabApplication.java
    common/ApiExceptionHandler.java
    note/
      NoteController.java
      NoteService.java
      NoteRepository.java
      Note.java
      CreateNoteRequest.java
      NoteResponse.java
      NoteNotFoundException.java
  src/main/resources/application.properties
  src/test/java/com/deploy_lab/NotesApiTests.java
  src/test/resources/application-test.properties
```

Request flow: HTTP -> Controller -> Service -> Repository -> MySQL.

## DevOps practice checklist

- [ ] Run the application with MySQL and call all four endpoints.
- [ ] Confirm invalid input returns `400` and a missing note returns `404`.
- [ ] Create a note, record its ID, restart the application, and retrieve it again to verify persistence.
- [ ] Run the tests and build the executable JAR.
- [ ] Run the application from the JAR.
- [ ] Use this source for subsequent Docker, Compose, CI/CD, and deployment exercises.

For the first lab, Hibernate manages the schema through `spring.jpa.hibernate.ddl-auto=update`. A later exercise can introduce versioned migrations and switch to `validate`.

The API has no authentication and returns the complete note list without pagination. The database initialization script targets local connections. A remote or containerized database requires an appropriate MySQL user/host configuration and JDBC URL.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| `release version 24 not supported` | Set `JAVA_HOME` and `PATH` to JDK 24, then check the Java version reported by Maven. |
| Required `.env` resource does not exist | Create `.env` from `.env.example` and run from the project root. |
| `DB_PASSWORD` cannot be resolved | Add the password to `.env` or set the environment variable. |
| `Access denied for user` | Check the username, password, and MySQL user/host permissions. |
| `Communications link failure` | Check that MySQL is running and the JDBC host and port are correct. |
| Port 8080 is already in use | Stop the previous instance or set `SERVER_PORT=8081` and use that port for requests. |
| `mysql` command is not found | Use MySQL Workbench or add the MySQL `bin` directory to `PATH`. |

If a local MySQL connection without TLS reports `Public Key Retrieval is not allowed`, the following URL can be used for the local lab:

```properties
DB_URL=jdbc:mysql://localhost:3306/deploy_lab?connectionTimeZone=UTC&sslMode=DISABLED&allowPublicKeyRetrieval=true
```

Configure TLS appropriately for deployed environments instead of using this local-only connection setting.
