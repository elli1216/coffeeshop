import { useEffect, useState } from "react";
import { getProducts } from "../api/productApi";
import { getOrders } from "../api/orderApi";
import { getCategories } from '../api/categoryApi';

type Status = "loading" | "ok" | "error";

interface CheckResult {
  name: string;
  endpoint: string;
  status: Status;
  count?: number;
  message?: string;
}

const checks = [
  { name: "Categories", endpoint: "GET /categories", run: getCategories },
  { name: "Products", endpoint: "GET /products", run: () => getProducts() },
  { name: "Orders", endpoint: "GET /orders", run: () => getOrders() },
];

export function Welcome() {
  const [results, setResults] = useState<CheckResult[]>(
    checks.map((c) => ({ name: c.name, endpoint: c.endpoint, status: "loading" }))
  );

  useEffect(() => {
    checks.forEach((check, index) => {
      check
        .run()
        .then((data) => update(index, { status: "ok", count: data.length }))
        .catch((e: Error) => update(index, { status: "error", message: e.message }));
    });
  }, []);

  const update = (index: number, patch: Partial<CheckResult>) =>
    setResults((prev) => prev.map((r, i) => (i === index ? { ...r, ...patch } : r)));

  const icon: Record<Status, string> = { loading: "⏳", ok: "✅", error: "❌" };

  return (
    <main style={{ fontFamily: "sans-serif", padding: "2rem" }}>
      <h1>Backend Connection Test</h1>
      <p>
        API URL: <code>{import.meta.env.VITE_API_URL}</code>
      </p>

      <table cellPadding={8} style={{ borderCollapse: "collapse" }}>
        <thead>
          <tr>
            <th align="left">Check</th>
            <th align="left">Endpoint</th>
            <th align="left">Result</th>
          </tr>
        </thead>
        <tbody>
          {results.map((r) => (
            <tr key={r.name}>
              <td>{icon[r.status]} {r.name}</td>
              <td><code>{r.endpoint}</code></td>
              <td style={{ color: r.status === "error" ? "red" : "inherit" }}>
                {r.status === "loading" && "Checking..."}
                {r.status === "ok" && `${r.count} record(s)`}
                {r.status === "error" && r.message}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </main>
  );
}