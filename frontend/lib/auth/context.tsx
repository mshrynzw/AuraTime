"use client";

import { MeResponse, authApi } from "@/lib/api/auth";
import Cookies from "js-cookie";
import { useRouter } from "next/navigation";
import React, { createContext, useContext, useEffect, useState } from "react";

interface AuthContextType {
  user: MeResponse | null;
  loading: boolean;
  login: (token: string) => void;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  const login = (token: string) => {
    Cookies.set("auth_token", token, { expires: 1 }); // 1日
    refreshUser();
  };

  const logout = () => {
    Cookies.remove("auth_token");
    setUser(null);
    router.push("/login");
  };

  const refreshUser = async () => {
    try {
      const token = Cookies.get("auth_token");
      if (!token) {
        setUser(null);
        setLoading(false);
        return;
      }

      const response = await authApi.getMe();
      if (response.success) {
        setUser(response.data);
      } else {
        setUser(null);
        Cookies.remove("auth_token");
      }
    } catch (error) {
      console.error("Failed to fetch user", error);
      setUser(null);
      Cookies.remove("auth_token");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshUser();
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
