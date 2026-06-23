"use client";

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react";
import { api, type ApiResponse } from "@/lib/api";
import type { User } from "@/types";

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => void;
  hasPermission: (perm: string) => boolean;
  hasRole: (role: string) => boolean;
}

interface RegisterData {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

interface TokenResponse {
  token: string;
  userId: string;
  email: string;
  roles: string[];
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const loadUser = useCallback(async () => {
    try {
      const token = api.getToken();
      if (!token) { setLoading(false); return; }

      const data = await api.get<{ userId: string; email: string; firstName: string; lastName: string; roles: string[]; permissions: string[] }>("/api/v1/auth/me");
      setUser({ id: data.userId, email: data.email, firstName: data.firstName, lastName: data.lastName, roles: data.roles, permissions: data.permissions });
    } catch {
      api.setToken(null);
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadUser(); }, [loadUser]);

  const login = async (email: string, password: string) => {
    const data = await api.post<TokenResponse>("/api/v1/auth/login", { email, password });
    api.setToken(data.token);
    setUser({ id: data.userId, email: data.email, firstName: "", lastName: "", roles: data.roles, permissions: [] });
    await loadUser();
  };

  const register = async (data: RegisterData) => {
    const res = await api.post<TokenResponse>("/api/v1/auth/register", data);
    api.setToken(res.token);
    setUser({ id: res.userId, email: res.email, firstName: data.firstName, lastName: data.lastName, roles: res.roles, permissions: [] });
    await loadUser();
  };

  const logout = () => { api.setToken(null); setUser(null); window.location.href = "/auth/login"; };

  const hasPermission = (perm: string) => user?.permissions.includes(perm) ?? false;
  const hasRole = (role: string) => user?.roles.includes(role) ?? false;

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, hasPermission, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
};
