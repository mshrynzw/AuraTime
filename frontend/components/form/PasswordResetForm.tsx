"use client";

import { Button } from "@/components/ui/button";
import { authApi } from "@/lib/api/auth";
import {
  passwordResetConfirmSchema,
  passwordResetRequestSchema,
  type PasswordResetConfirmFormData,
  type PasswordResetRequestFormData,
} from "@/lib/validation/schemas";
import { zodResolver } from "@hookform/resolvers/zod";
import { Eye, EyeOff } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";

export default function PasswordResetForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token");
  const [step, setStep] = useState<"request" | "confirm">(
    token ? "confirm" : "request"
  );
  const [showPassword, setShowPassword] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // パスワードリセット要求フォーム
  const requestForm = useForm<PasswordResetRequestFormData>({
    resolver: zodResolver(passwordResetRequestSchema),
    mode: "onBlur",
  });

  // パスワードリセット実行フォーム
  const confirmForm = useForm<PasswordResetConfirmFormData>({
    resolver: zodResolver(passwordResetConfirmSchema),
    mode: "onBlur",
    defaultValues: {
      token: token || "",
    },
  });

  const onRequestSubmit = async (data: PasswordResetRequestFormData) => {
    try {
      const response = await authApi.requestPasswordReset(data);
      if (response.success) {
        setSuccessMessage(
          "パスワードリセットメールを送信しました。メールをご確認ください。"
        );
      } else {
        requestForm.setError("root", {
          message: "パスワードリセット要求に失敗しました",
        });
      }
    } catch (err: any) {
      const errorMessage =
        err.response?.data?.error?.message ||
        "パスワードリセット要求に失敗しました";
      requestForm.setError("root", { message: errorMessage });
    }
  };

  const onConfirmSubmit = async (data: PasswordResetConfirmFormData) => {
    try {
      const response = await authApi.confirmPasswordReset(data);
      if (response.success) {
        setSuccessMessage(
          "パスワードをリセットしました。ログイン画面に移動します。"
        );
        setTimeout(() => {
          router.push("/login");
        }, 2000);
      } else {
        confirmForm.setError("root", {
          message: "パスワードリセットに失敗しました",
        });
      }
    } catch (err: any) {
      const errorMessage =
        err.response?.data?.error?.message ||
        "パスワードリセットに失敗しました";
      confirmForm.setError("root", { message: errorMessage });
    }
  };

  if (step === "request") {
    return (
      <form
        className="space-y-6"
        onSubmit={requestForm.handleSubmit(onRequestSubmit)}
      >
        {successMessage && (
          <div className="text-xs bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded">
            {successMessage}
          </div>
        )}
        {requestForm.formState.errors.root && (
          <div className="text-xs bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
            {requestForm.formState.errors.root.message}
          </div>
        )}

        <div>
          <label
            htmlFor="email"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            メールアドレス
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            {...requestForm.register("email")}
            className={`appearance-none relative block w-full px-3 py-2 border ${
              requestForm.formState.errors.email
                ? "border-red-300 focus:border-red-500 focus:ring-red-500"
                : "border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
            } placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:z-10 sm:text-sm`}
            placeholder="メールアドレス"
          />
          {requestForm.formState.errors.email && (
            <p className="mt-1 text-sm text-red-600">
              {requestForm.formState.errors.email.message}
            </p>
          )}
        </div>

        <div className="space-y-3">
          <Button
            type="submit"
            variant="default"
            className="w-full"
            disabled={requestForm.formState.isSubmitting}
          >
            {requestForm.formState.isSubmitting
              ? "送信中..."
              : "リセットメールを送信"}
          </Button>
          <Button
            type="button"
            variant="link"
            className="w-full"
            onClick={() => router.push("/login")}
          >
            ログイン画面に戻る
          </Button>
        </div>
      </form>
    );
  }

  return (
    <form
      className="space-y-6"
      onSubmit={confirmForm.handleSubmit(onConfirmSubmit)}
    >
      {successMessage && (
        <div className="text-xs bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded">
          {successMessage}
        </div>
      )}
      {confirmForm.formState.errors.root && (
        <div className="text-xs bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {confirmForm.formState.errors.root.message}
        </div>
      )}

      <div>
        <label
          htmlFor="token"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          リセットトークン
        </label>
        <input
          id="token"
          type="text"
          {...confirmForm.register("token")}
          className={`appearance-none relative block w-full px-3 py-2 border ${
            confirmForm.formState.errors.token
              ? "border-red-300 focus:border-red-500 focus:ring-red-500"
              : "border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
          } placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:z-10 sm:text-sm`}
          placeholder="リセットトークン"
        />
        {confirmForm.formState.errors.token && (
          <p className="mt-1 text-sm text-red-600">
            {confirmForm.formState.errors.token.message}
          </p>
        )}
      </div>

      <div className="relative">
        <label
          htmlFor="newPassword"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          新しいパスワード
        </label>
        <input
          id="newPassword"
          type={showPassword ? "text" : "password"}
          autoComplete="new-password"
          {...confirmForm.register("newPassword")}
          className={`appearance-none relative block w-full px-3 py-2 pr-10 border ${
            confirmForm.formState.errors.newPassword
              ? "border-red-300 focus:border-red-500 focus:ring-red-500"
              : "border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
          } placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:z-10 sm:text-sm`}
          placeholder="新しいパスワード（8文字以上）"
        />
        <button
          type="button"
          onClick={() => setShowPassword(!showPassword)}
          className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-500 hover:text-gray-700 focus:outline-none top-6"
          aria-label={showPassword ? "パスワードを非表示" : "パスワードを表示"}
        >
          {showPassword ? (
            <EyeOff className="h-5 w-5" />
          ) : (
            <Eye className="h-5 w-5" />
          )}
        </button>
        {confirmForm.formState.errors.newPassword && (
          <p className="mt-1 text-sm text-red-600">
            {confirmForm.formState.errors.newPassword.message}
          </p>
        )}
      </div>

      <div className="space-y-3">
        <Button
          type="submit"
          variant="default"
          className="w-full"
          disabled={confirmForm.formState.isSubmitting}
        >
          {confirmForm.formState.isSubmitting
            ? "リセット中..."
            : "パスワードをリセット"}
        </Button>
        <Button
          type="button"
          variant="link"
          className="w-full"
          onClick={() => router.push("/login")}
        >
          ログイン画面に戻る
        </Button>
      </div>
    </form>
  );
}
