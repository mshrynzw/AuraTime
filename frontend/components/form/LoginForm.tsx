"use client";

import { Button } from "@/components/ui/button";
import { authApi } from "@/lib/api/auth";
import { useAuth } from "@/lib/auth/context";
import { loginSchema, type LoginFormData } from "@/lib/validation/schemas";
import { zodResolver } from "@hookform/resolvers/zod";
import { Eye, EyeOff } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";

export default function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuth();
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    const registered = searchParams.get("registered");
    if (registered === "true") {
      setSuccessMessage("ユーザー登録が完了しました。ログインしてください。");
      // URLからクエリパラメータを削除
      router.replace("/login", { scroll: false });
    }
  }, []);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    mode: "onBlur", // フォーカスが外れたときにバリデーション
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      const response = await authApi.login(data);
      if (response.success && response.data) {
        login(response.data.token);
        router.push("/dashboard");
      } else {
        setError("root", { message: "ログインに失敗しました" });
      }
    } catch (err: any) {
      const errorMessage =
        err.response?.data?.error?.message || "ログインに失敗しました";
      setError("root", { message: errorMessage });
    }
  };

  return (
    <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
      {/* 成功メッセージ */}
      {successMessage && (
        <div className="text-xs bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded">
          {successMessage}
        </div>
      )}
      {/* エラーメッセージ */}
      {errors.root && (
        <div className="text-xs bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {errors.root.message}
        </div>
      )}

      <div className="rounded-md shadow-sm -space-y-px">
        <div>
          <label htmlFor="email" className="sr-only">
            メールアドレス
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            {...register("email")}
            className={`appearance-none relative block w-full px-3 py-2 border bg-white opacity-100 ${
              errors.email
                ? "border-red-300 focus:border-red-500 focus:ring-red-500 rounded-md"
                : "border-gray-300 focus:ring-indigo-500 focus:border-indigo-500 rounded-t-md"
            } placeholder-gray-500 text-gray-900 focus:outline-none focus:z-10 sm:text-sm`}
            placeholder="メールアドレス"
          />
          {errors.email && (
            <p className="mb-1 text-sm text-red-600">{errors.email.message}</p>
          )}
        </div>
        <div className="relative">
          <label htmlFor="password" className="sr-only">
            パスワード
          </label>
          <input
            id="password"
            type={showPassword ? "text" : "password"}
            autoComplete="current-password"
            {...register("password")}
            className={`appearance-none relative block w-full px-3 py-2 pr-10 border ${
              errors.password
                ? "border-red-300 focus:border-red-500 focus:ring-red-500 rounded-md"
                : "border-gray-300 focus:ring-indigo-500 focus:border-indigo-500 rounded-b-md"
            } placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:z-10 sm:text-sm`}
            placeholder="パスワード"
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-500 hover:text-gray-700 focus:outline-none"
            aria-label={
              showPassword ? "パスワードを非表示" : "パスワードを表示"
            }
          >
            {showPassword ? (
              <EyeOff className="h-5 w-5" />
            ) : (
              <Eye className="h-5 w-5" />
            )}
          </button>
          {errors.password && (
            <p className="mb-1 text-sm text-red-600">
              {errors.password.message}
            </p>
          )}
        </div>
      </div>

      <div className="space-y-3">
        <Button
          type="submit"
          variant="default"
          className="w-full"
          disabled={isSubmitting}
        >
          {isSubmitting ? "ログイン中..." : "ログイン"}
        </Button>
        <Button
          type="button"
          variant="link"
          className="w-full"
          onClick={() => router.push("/register")}
        >
          新規登録はこちら
        </Button>
      </div>
    </form>
  );
}
