import { api } from "./api";

describe("ApiClient", () => {
  beforeEach(() => {
    localStorage.clear();
    api.setToken(null);
  });

  describe("setToken / getToken", () => {
    it("stores token in localStorage", () => {
      api.setToken("test-token-123");
      expect(localStorage.getItem("token")).toBe("test-token-123");
      expect(api.getToken()).toBe("test-token-123");
    });

    it("removes token when set to null", () => {
      localStorage.setItem("token", "old-token");
      api.setToken(null);
      expect(localStorage.getItem("token")).toBeNull();
      expect(api.getToken()).toBeNull();
    });

    it("returns null when no token stored", () => {
      expect(api.getToken()).toBeNull();
    });
  });

  describe("request methods", () => {
    const mockFetch = jest.fn();
    beforeEach(() => {
      global.fetch = mockFetch;
      mockFetch.mockReset();
    });

    afterEach(() => {
      jest.restoreAllMocks();
    });

    it("get sends GET request with correct path", async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ responseCode: "00", data: { id: 1 } }),
      });

      const result = await api.get("/api/v1/test");
      expect(mockFetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/v1/test",
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toEqual({ id: 1 });
    });

    it("post sends POST with body", async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ responseCode: "00", data: { created: true } }),
      });

      const result = await api.post("/api/v1/test", { name: "test" });
      expect(mockFetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/v1/test",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ name: "test" }),
        })
      );
      expect(result).toEqual({ created: true });
    });

    it("includes Authorization header when token is set", async () => {
      api.setToken("my-token");
      mockFetch.mockResolvedValue({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ responseCode: "00", data: {} }),
      });

      await api.get("/api/v1/protected");
      expect(mockFetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          headers: expect.objectContaining({
            Authorization: "Bearer my-token",
          }),
        })
      );
    });

    it("throws on 4xx response", async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 400,
        json: () => Promise.resolve({ responseCode: "400", responseMessage: "Bad request" }),
      });

      await expect(api.get("/api/v1/error")).rejects.toThrow("Bad request");
    });

    it("redirects to login on 401", async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 401,
        json: () => Promise.resolve({ responseCode: "401", responseMessage: "Unauthorized" }),
      });

      await expect(api.get("/api/v1/unauthorized")).rejects.toThrow("Unauthorized");
      expect(localStorage.getItem("token")).toBeNull();
    });
  });
});
