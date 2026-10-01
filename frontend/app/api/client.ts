import axios, { AxiosError } from "axios";
import type { ProblemDetail } from "../types";

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: { "Content-Type": "application/json" },
});

client.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ProblemDetail>) => {
    const data = error.response?.data;
    const message = data?.errors
      ? Object.entries(data.errors)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join(", ")
      : (data?.detail ?? "Cannot reach the server. Is the backend running?");
    return Promise.reject(new Error(message));
  },
);

export default client;
