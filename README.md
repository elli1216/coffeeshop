# ☕ Coffeeshop — Full-Stack App with CI/CD

A coffeeshop ordering system with a **Spring Boot** REST API, a **React + TypeScript** frontend, and a fully containerized **CI/CD pipeline** (Gitea → Jenkins → SonarQube → Docker deploy).

| Part | Docs |
|---|---|
| Backend (Spring Boot API) | [`backend/README.md`](backend/README.md) |
| Frontend (React + TypeScript) | [`frontend/README.md`](frontend/README.md) |

---

## 1. Architecture

```text
 Developer (IntelliJ / VS Code)
        │ git push
        ▼
 ┌──────────────┐  webhook   ┌──────────────────────────────────────────────┐
 │    Gitea     │──────────► │                  Jenkins                     │
 │  (Git server)│ ◄───────── │  checkout → build/test → Sonar → Quality Gate│
 └──────────────┘   clone    │  → deploy (docker compose) → smoke test      │
                             └───────┬───────────────────────┬──────────────┘
                                     │ analysis              │ docker.sock
                                     ▼                       ▼
                             ┌──────────────┐     ┌────────────────────────┐
                             │  SonarQube   │     │ coffeeshop-frontend    │
                             │ (code quality)│    │ (nginx, React build)   │
                             └──────────────┘     │          │ /api        │
                                                  │          ▼             │
                                                  │ coffeeshop-backend     │
                                                  │ (Spring Boot)          │
                                                  └──────────┬─────────────┘
                                                             ▼
                                                  ┌────────────────────────┐
                                                  │   MySQL (db)           │
                                                  │ databases: gitea,      │
                                                  │            coffeeshop  │
                                                  └────────────────────────┘
```

All containers share one Docker network (`gitea-docker_gitea`), so they reach each other **by name**.

---

## 2. Tools and platforms

| Category | Tool | Purpose |
|---|---|---|
| Backend | Java 21, Spring Boot 4, Spring Data JPA, Hibernate, Lombok, Bean Validation | REST API and data access |
| Frontend | React, TypeScript, React Router 7 (SPA mode), Vite, Axios | User interface |
| Database | MySQL 8 | Persistent storage |
| Build | Maven, npm | Build and dependency management |
| Source control | Git + **Gitea** (self-hosted) | Repository hosting and webhooks |
| CI/CD | **Jenkins** (`lts-jdk21`, custom image with Docker CLI) | Pipeline automation |
| Code quality | **SonarQube** Community | Static analysis and Quality Gates |
| Containers | Docker, Docker Compose (inside WSL Ubuntu) | Runs all services |
| Web server | nginx | Serves the React build |
| Dev tools | IntelliJ IDEA, VS Code, Postman/Bruno, MySQL Workbench | Development and testing |

---

## 3. Repository structure

```text
coffeeshop/
├── Jenkinsfile                  # CI/CD pipeline
├── deploy/
│   └── docker-compose.app.yml   # deploys backend + frontend containers
├── backend/                     # Spring Boot API  (see backend/README.md)
│   ├── Dockerfile
│   └── pom.xml
└── frontend/                    # React app        (see frontend/README.md)
    ├── Dockerfile
    ├── nginx.conf
    └── sonar-project.properties
```

The infrastructure compose file (Gitea, MySQL, SonarQube, Jenkins) lives outside the repo in `~/gitea-docker/`.

---

## 4. Ports and addresses

> **Rule:** inside a container, `localhost` means *that container*. Containers use **service names + internal ports**; your browser uses **`localhost` + published ports**.

| Service | From your laptop/browser | From another container |
|---|---|---|
| Gitea | `http://localhost:3000` | `http://gitea:3000` |
| Jenkins | `http://localhost:8082` | `http://jenkins:8080` |
| SonarQube | `http://localhost:9000` | `http://sonarqube:9000` |
| MySQL (Docker) | `localhost:3307` | `db:3306` |
| Backend (deployed) | `http://localhost:8081` | `http://coffeeshop-backend:8081` |
| Frontend (deployed) | `http://localhost:8083` | `http://coffeeshop-frontend:80` |
| Frontend (Vite dev) | `http://localhost:5173` | — |

---

## 5. Infrastructure setup (`~/gitea-docker/`)

### 5.1 `docker-compose.yml`

```yaml
networks:
  gitea:
    external: false

services:
  server:                                   # Gitea
    image: docker.gitea.com/gitea:28.0.0
    container_name: gitea
    environment:
      - USER_UID=1000
      - USER_GID=1000
      - GITEA__database__DB_TYPE=mysql
      - GITEA__database__HOST=db:3306
      - GITEA__database__NAME=gitea
      - GITEA__database__USER=gitea
      - GITEA__database__PASSWD=gitea
      - GITEA__webhook__ALLOWED_HOST_LIST=jenkins   # allow webhooks to Jenkins
    restart: always
    networks: [gitea]
    volumes:
      - ./gitea:/data
      - /etc/timezone:/etc/timezone:ro
      - /etc/localtime:/etc/localtime:ro
    ports:
      - "3000:3000"
      - "222:22"
    depends_on: [db]

  db:                                       # MySQL
    image: docker.io/library/mysql:8
    restart: always
    environment:
      - MYSQL_ROOT_PASSWORD=gitea
      - MYSQL_USER=gitea
      - MYSQL_PASSWORD=gitea
      - MYSQL_DATABASE=gitea
    networks: [gitea]
    volumes:
      - ./mysql:/var/lib/mysql
    ports:
      - "3307:3306"                         # 3306 is used by Windows MySQL

  sonarqube:
    image: sonarqube:community
    container_name: sonarqube
    restart: always
    networks: [gitea]
    ports:
      - "9000:9000"
    volumes:
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_extensions:/opt/sonarqube/extensions

  jenkins:
    build:
      context: .
      dockerfile: Dockerfile.jenkins
    container_name: jenkins
    restart: always
    group_add:
      - "0"                                 # group of docker.sock (snap Docker = root)
    networks: [gitea]
    ports:
      - "8082:8080"                         # 8080 is used by local Jenkins
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock

volumes:
  sonarqube_data:
  sonarqube_extensions:
  jenkins_home:
```

### 5.2 `Dockerfile.jenkins`

Adds the Docker CLI and Compose plugin so Jenkins can deploy containers.

```dockerfile
FROM jenkins/jenkins:lts-jdk21
USER root
COPY --from=docker:cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker/compose-bin:latest /docker-compose /usr/libexec/docker/cli-plugins/docker-compose
USER jenkins
```

### 5.3 Start the infrastructure

```bash
sudo sysctl -w vm.max_map_count=262144        # required by SonarQube
cd ~/gitea-docker
sudo docker compose config                     # validate YAML first
sudo docker compose up -d --build
sudo docker compose exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### 5.4 Application database (one time, as MySQL root)

```bash
sudo docker compose exec db mysql -uroot -p
```

```sql
CREATE DATABASE IF NOT EXISTS coffeeshop;
CREATE USER IF NOT EXISTS 'coffee'@'%' IDENTIFIED BY 'coffee';
GRANT ALL PRIVILEGES ON coffeeshop.* TO 'coffee'@'%';
FLUSH PRIVILEGES;
```

---

## 6. SonarQube configuration (`http://localhost:9000`)

1. Log in as `admin` / `admin` and change the password.
2. **Token:** My Account → Security → Generate Token (User Token). Copy it — it is shown once.
3. **Webhook:** Administration → Configuration → Webhooks → Create
   - URL: `http://jenkins:8080/sonarqube-webhook/` *(trailing slash required)*
4. Projects are created automatically on first analysis:
   - `coffeeshop-backend` — key passed by `-D` flags in the Jenkinsfile
   - `coffeeshop-frontend` — key in `frontend/sonar-project.properties`

---

## 7. Jenkins configuration (`http://localhost:8082`)

| Area | Setting | Value / name |
|---|---|---|
| Plugins | SonarQube Scanner, NodeJS (+ suggested plugins) | — |
| Credentials | Secret text — SonarQube token | ID `sonar-token` |
| Credentials | Username/password — Gitea login | ID `gitea-creds` |
| System → SonarQube servers | Name / URL / token | `SonarQube` / `http://sonarqube:9000` / `sonar-token` |
| Tools → Maven | Install automatically | `Maven3` |
| Tools → NodeJS | Install automatically, **24.x LTS** | `Node24` |
| Tools → SonarQube Scanner | Install automatically | `SonarScanner` |
| Security → Git plugin notifyCommit access tokens | Token for the Gitea webhook | name `coffeeshop` |

**Pipeline job `coffeeshop`:**
- Definition: *Pipeline script from SCM* → Git
- Repository URL: `http://gitea:3000/elli1216/coffeeshop.git`, credentials `gitea-creds`
- Branch: `*/main`, Script path: `Jenkinsfile`
- Build Triggers: **Poll SCM** with an empty schedule (triggered by webhook)

> Tool and server names must match the `Jenkinsfile` exactly.

---

## 8. CI/CD pipeline (`Jenkinsfile`)

| # | Stage | What it does |
|---|---|---|
| 1 | Checkout | Clones the repo from Gitea |
| 2 | Backend: Build & Test | `mvn -B clean verify` (compiles + runs JUnit tests) |
| 3 | Backend: SonarQube Analysis | Maven Sonar plugin, project `coffeeshop-backend` |
| 4 | Backend: Quality Gate | `waitForQualityGate abortPipeline: true` |
| 5 | Frontend: Install & Build | `npm ci`, `npm run build` (includes TypeScript check), retried up to 3× |
| 6 | Frontend: SonarQube Analysis | `sonar-scanner` using `sonar-project.properties` |
| 7 | Frontend: Quality Gate | `waitForQualityGate abortPipeline: true` |
| 8 | Deploy | `docker compose -p coffeeshop-app -f deploy/docker-compose.app.yml up -d --build` |
| 9 | Smoke Test | Polls `/actuator/health` until `UP`, then checks the frontend responds |

Key environment variables:

```groovy
environment {
    SPRING_DATASOURCE_URL = 'jdbc:mysql://db:3306/coffeeshop'  // overrides application.properties in CI
    VITE_API_URL          = 'http://localhost:8081/api'         // or '/api' when using the nginx proxy
}
```

### Deployment (`deploy/docker-compose.app.yml`)

```yaml
services:
  backend:
    build: ../backend
    container_name: coffeeshop-backend
    restart: unless-stopped
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/coffeeshop
    ports: ["8081:8081"]
    networks: [shared]

  frontend:
    build: ../frontend
    container_name: coffeeshop-frontend
    restart: unless-stopped
    ports: ["8083:80"]
    depends_on: [backend]
    networks: [shared]

networks:
  shared:
    external: true
    name: gitea-docker_gitea    # check with: sudo docker network ls
```

`-p coffeeshop-app` gives the app its own Compose project, so redeploys never touch Gitea, Jenkins, SonarQube, or MySQL.

---

## 9. Build on push (Gitea webhook)

Repo → Settings → Webhooks → Add Webhook → Gitea

- Target URL:
  `http://jenkins:8080/git/notifyCommit?url=http://gitea:3000/elli1216/coffeeshop.git&token=<NOTIFY_COMMIT_TOKEN>`
- Trigger on: Push events, branch filter `main`

Requirements: **Poll SCM** enabled on the job, and `GITEA__webhook__ALLOWED_HOST_LIST` set on Gitea so it may call the private Docker address of Jenkins.

> Never commit tokens. Regenerate any token that has been shared.

---

## 10. Verify a deployment

| Check | URL |
|---|---|
| Frontend | http://localhost:8083 |
| Backend health | http://localhost:8081/actuator/health → `{"status":"UP"}` |
| API sample | http://localhost:8081/api/products |
| SonarQube projects | http://localhost:9000 |

---

## 11. Troubleshooting reference

| Symptom | Cause | Fix |
|---|---|---|
| Jenkins can't reach `localhost:3000` | `localhost` = the Jenkins container | Use `gitea:3000` |
| `Communications link failure` in CI tests | Used host port `3307` inside Docker | Use `db:3306` |
| `Access denied for user ... to database` | Missing `GRANT` for that DB name | Grant on the exact database |
| YAML `could not find expected ':'` | Tabs / invisible non-breaking spaces | Retype with spaces; run `docker compose config` |
| `libatomic.so.1` missing | Node 25+ on the Debian Jenkins image | Use Node 24 LTS |
| `docker: not found` in Deploy | Stock Jenkins image | Use `Dockerfile.jenkins` and rebuild |
| `/var/run/docker.sock: no such file` | Socket not mounted | Add the volume; `up -d --force-recreate jenkins` |
| `stat dist: file does not exist` | React Router outputs to `build/client` | `COPY build/client ...` |
| `Prerender: Request failed ... ECONNREFUSED` | React Router 8 prerender in CI | Pinned React Router 7.18.1 + `retry(3)` |
| Quality Gate stage hangs | Missing SonarQube webhook | Add `http://jenkins:8080/sonarqube-webhook/` |
| Webhook `denied by egress policy` | Gitea blocks private IPs | Set `GITEA__webhook__ALLOWED_HOST_LIST` |
| `coffeeshop-backend:8081` won't open in browser | Container names only work inside Docker | Use `localhost:8081` |
