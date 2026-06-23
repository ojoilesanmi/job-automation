const BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export interface ApiResponse<T> {
  responseCode: string;
  responseStatus: string;
  responseMessage: string;
  data: T;
}

class ApiClient {
  private token: string | null = null;

  setToken(token: string | null) {
    this.token = token;
    if (token) localStorage.setItem("token", token);
    else localStorage.removeItem("token");
  }

  getToken(): string | null {
    if (typeof window === "undefined") return null;
    if (!this.token) this.token = localStorage.getItem("token");
    return this.token;
  }

  private async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const headers: Record<string, string> = { "Content-Type": "application/json" };
    const token = this.getToken();
    if (token) headers["Authorization"] = `Bearer ${token}`;

    const res = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });

    const json: ApiResponse<T> = await res.json();

    if (res.status === 401) {
      this.setToken(null);
      if (typeof window !== "undefined") window.location.href = "/auth/login";
      throw new Error("Unauthorized");
    }

    if (res.status >= 400) {
      throw new Error(json.responseMessage || "Request failed");
    }

    return json.data;
  }

  get<T>(path: string) { return this.request<T>("GET", path); }
  post<T>(path: string, body?: unknown) { return this.request<T>("POST", path, body); }
  put<T>(path: string, body?: unknown) { return this.request<T>("PUT", path, body); }
  del<T>(path: string) { return this.request<T>("DELETE", path); }

  async upload<T>(path: string, file: File, fieldName = "file"): Promise<T> {
    const headers: Record<string, string> = {};
    const token = this.getToken();
    if (token) headers["Authorization"] = `Bearer ${token}`;

    const form = new FormData();
    form.append(fieldName, file);

    const res = await fetch(`${BASE}${path}`, { method: "POST", headers, body: form });

    if (res.status === 401) {
      this.setToken(null);
      if (typeof window !== "undefined") window.location.href = "/auth/login";
      throw new Error("Unauthorized");
    }

    const json: ApiResponse<T> = await res.json();
    if (res.status >= 400) throw new Error(json.responseMessage || "Upload failed");
    return json.data;
  }

  async postForm<T>(path: string, fields: Record<string, string>): Promise<T> {
    const headers: Record<string, string> = {};
    const token = this.getToken();
    if (token) headers["Authorization"] = `Bearer ${token}`;

    const form = new FormData();
    Object.entries(fields).forEach(([k, v]) => form.append(k, v));

    const res = await fetch(`${BASE}${path}`, { method: "POST", headers, body: form });
    const json: ApiResponse<T> = await res.json();

    if (res.status >= 400) throw new Error(json.responseMessage || "Request failed");
    return json.data;
  }
}

export const api = new ApiClient();
