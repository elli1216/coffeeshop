# ☕ Coffeeshop — Frontend (React + TypeScript)

Web UI for the Coffeeshop API. Built with **React**, **TypeScript**, **React Router 7 (framework mode, SPA)**, **Vite**, and **Axios**, and served in production by **nginx**.

---

## 1. Tech stack

| Tool                                                   | Purpose                               |
| ------------------------------------------------------ | ------------------------------------- |
| React + TypeScript                                     | UI with type safety                   |
| React Router **7.18.1** (framework mode, `ssr: false`) | Routing; builds a static SPA          |
| Vite                                                   | Dev server and bundler                |
| Axios                                                  | HTTP client for the backend API       |
| nginx (alpine)                                         | Serves the production build in Docker |

> React Router is pinned to **7.18.1**. Version 8 prerenders through a temporary preview server, which failed in the Jenkins container (`Prerender: Request failed ... ECONNREFUSED`). All `react-router` / `@react-router/*` packages must use the **same version**.

---

## 2. Project structure

```text
frontend/
├── app/                       # application code (routes, components, API, types)
│   ├── routes/                # pages
│   ├── api/                   # axios client + typed API modules
│   └── types/                 # interfaces matching the backend DTOs
├── public/
├── build/                     # output of `npm run build` (not committed)
│   └── client/                # static files served by nginx
├── react-router.config.ts     # ssr: false → SPA mode
├── vite.config.ts
├── tsconfig.json
├── vite.env.d.ts              # types for import.meta.env
├── .env                       # local only (see .env.example)
├── Dockerfile
├── nginx.conf
├── sonar-project.properties
└── package.json / package-lock.json
```

---

## 3. Configuration

### 3.1 SPA mode — `react-router.config.ts`

```typescript
import type { Config } from "@react-router/dev/config";

export default {
  ssr: false,
} satisfies Config;
```

With `ssr: false`, the build generates `build/client/index.html`, which nginx can serve as static files.

### 3.2 Environment variables

`.env` (local development):

```env
VITE_API_URL=http://localhost:8081/api
```

- Only variables prefixed with `VITE_` are exposed to the browser.
- The value is **baked in at build time**; restart the dev server after changing it.
- In CI, `VITE_API_URL` is set in the Jenkinsfile (`http://localhost:8081/api`, or `/api` when nginx proxies the API).

Typed in `vite.env.d.ts`:

```typescript
/// <reference types="vite/client" />
interface ImportMetaEnv {
  readonly VITE_API_URL: string;
}
interface ImportMeta {
  readonly env: ImportMetaEnv;
}
```

---

## 4. API layer

### Types (mirror the backend DTOs)

| Java type          | TypeScript type                                          |
| ------------------ | -------------------------------------------------------- |
| `Long` / `Integer` | `number`                                                 |
| `BigDecimal`       | `number`                                                 |
| `LocalDateTime`    | `string` (ISO)                                           |
| `OrderStatus` enum | `"PENDING" \| "PREPARING" \| "COMPLETED" \| "CANCELLED"` |

String unions are used instead of TS `enum` because the Vite TypeScript template enables `erasableSyntaxOnly`. Use `import type { ... }` for type-only imports (`verbatimModuleSyntax`).

### Axios client

```typescript
import axios, { AxiosError } from "axios";
import type { ProblemDetail } from "../types";

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: { "Content-Type": "application/json" },
});

// Converts backend ProblemDetail errors into readable messages
client.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ProblemDetail>) => {
    const data = error.response?.data;
    const message = data?.errors
      ? Object.entries(data.errors)
          .map(([f, m]) => `${f}: ${m}`)
          .join(", ")
      : (data?.detail ?? "Cannot reach the server. Is the backend running?");
    return Promise.reject(new Error(message));
  },
);

export default client;
```

API modules: `categoryApi.ts`, `productApi.ts`, `orderApi.ts` — one typed function per backend endpoint (see `backend/README.md`).

---

## 5. Running locally

**Prerequisites:** Node.js 24 LTS, backend running on `http://localhost:8081`.

> On the Accenture laptop, PowerShell blocks `npm.ps1`. Use **`npm.cmd`** (or Command Prompt).

```powershell
npm.cmd install
npm.cmd run dev        # http://localhost:5173
npm.cmd run build      # TypeScript check + production build → build/client
```

If the browser shows a CORS error, confirm the backend `WebConfig` allows `http://localhost:5173`.

---

## 6. Docker image

### `Dockerfile`

```dockerfile
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY build/client /usr/share/nginx/html
EXPOSE 80
```

The `build/client` folder is produced by the pipeline's **Frontend: Install & Build** stage. `.dockerignore` must **not** exclude `build` (excluding `node_modules` is fine).

### `nginx.conf`

```nginx
server {
  listen 80;
  root /usr/share/nginx/html;

  # Optional: forward API calls to the backend container (same-origin, no CORS)
  location /api/ {
    proxy_pass http://coffeeshop-backend:8081;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
  }

  # SPA fallback: every route is handled by React Router
  location / {
    try_files $uri /index.html;
  }
}
```

Without the `/api/` block, `http://localhost:8083/api/...` returns the React `index.html`; call the API on `http://localhost:8081` instead.

Deployed URL: **http://localhost:8083**

---

## 7. CI/CD

| Stage                        | Command                                         |
| ---------------------------- | ----------------------------------------------- |
| Frontend: Install & Build    | `npm ci` then `npm run build` inside `retry(3)` |
| Frontend: SonarQube Analysis | `sonar-scanner`                                 |
| Frontend: Quality Gate       | `waitForQualityGate abortPipeline: true`        |

- `npm ci` requires a committed **`package-lock.json`**.
- Jenkins uses the NodeJS tool **`Node24`** (Node 25+ needs `libatomic1`, which the Jenkins image lacks).
- `npm run build` runs the TypeScript compiler, so type errors fail the pipeline.

---

## 8. Code quality (SonarQube)

`sonar-project.properties`:

```properties
sonar.projectKey=coffeeshop-frontend
sonar.projectName=coffeeshop-frontend
sonar.sources=app
sonar.exclusions=**/node_modules/**,build/**
```

The standalone scanner has no `pom.xml` to read, so this file tells it the project key and where the source code lives. The server URL and token are injected by `withSonarQubeEnv('SonarQube')` in the Jenkinsfile.
