"use client";

import { authApi } from "@/lib/api/auth";
import { useAuth } from "@/lib/auth/context";
import { loginSchema, type LoginFormData } from "@/lib/validation/schemas";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuth();
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    if (searchParams.get("registered") === "true") {
      setSuccessMessage("ユーザー登録が完了しました。ログインしてください。");
      // URLからクエリパラメータを削除
      router.replace("/login");
    }
  }, [searchParams, router]);

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
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full space-y-8 p-8 bg-white rounded-lg shadow-md">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            AuraTime
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            勤怠・給与管理システム
          </p>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit(onSubmit)}>
          {/* 成功メッセージ */}
          {successMessage && (
            <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded">
              {successMessage}
            </div>
          )}
          {/* エラーメッセージ */}
          {errors.root && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
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
                className={`appearance-none rounded-none relative block w-full px-3 py-2 border ${
                  errors.email
                    ? "border-red-300 focus:border-red-500 focus:ring-red-500"
                    : "border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
                } placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:z-10 sm:text-sm`}
                placeholder="メールアドレス"
              />
              {errors.email && (
                <p className="mt-1 text-sm text-red-600">
                  {errors.email.message}
                </p>
              )}
            </div>
            <div>
              <label htmlFor="password" className="sr-only">
                パスワード
              </label>
              <input
                id="password"
                type="password"
                autoComplete="current-password"
                {...register("password")}
                className={`appearance-none rounded-none relative block w-full px-3 py-2 border ${
                  errors.password
                    ? "border-red-300 focus:border-red-500 focus:ring-red-500"
                    : "border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
                } placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:z-10 sm:text-sm`}
                placeholder="パスワード"
              />
              {errors.password && (
                <p className="mt-1 text-sm text-red-600">
                  {errors.password.message}
                </p>
              )}
            </div>
          </div>

          <div className="space-y-3">
            <button
              type="submit"
              disabled={isSubmitting}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? "ログイン中..." : "ログイン"}
            </button>
            <button
              type="button"
              onClick={() => router.push("/register")}
              className="w-full text-center text-sm text-indigo-600 hover:text-indigo-800"
            >
              新規登録はこちら
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}


