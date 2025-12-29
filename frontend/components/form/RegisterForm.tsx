"use client";

import { Button } from "@/components/ui/button";
import { authApi } from "@/lib/api/auth";
import {
  registerSchema,
  type RegisterFormData,
} from "@/lib/validation/schemas";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";

export default function RegisterForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [invitationInfo, setInvitationInfo] = useState<any>(null);
  const [loadingInvitation, setLoadingInvitation] = useState(true);
  const isMountedRef = useRef(true);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
    watch,
    setValue,
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    mode: "onBlur",
  });

  const invitationToken = watch("invitationToken");
  const email = watch("email");

  // コンポーネントのアンマウント時にフラグを設定
  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  // URLパラメータから招待トークンを取得
  useEffect(() => {
    const token = searchParams.get("token");
    if (token) {
      // 現在の値と異なる場合のみsetValueを呼ぶ（無限ループを防ぐ）
      const currentToken = watch("invitationToken");
      if (currentToken !== token) {
        setValue("invitationToken", token, { shouldValidate: false });
        loadInvitationInfo(token);
      }
    } else {
      setLoadingInvitation(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  // 招待情報を取得
  const loadInvitationInfo = async (token: string) => {
    try {
      const response = await authApi.getInvitation(token);
      // アンマウント後の状態更新を防ぐ
      if (!isMountedRef.current) return;

      if (response.success && response.data) {
        setInvitationInfo(response.data);
        setValue("email", response.data.email);
        setLoadingInvitation(false);
      } else {
        setError("invitationToken", { message: "招待トークンが無効です" });
        setLoadingInvitation(false);
      }
    } catch (err: any) {
      // アンマウント後の状態更新を防ぐ
      if (!isMountedRef.current) return;

      const errorMessage =
        err.response?.data?.error?.message || "招待情報の取得に失敗しました";
      setError("invitationToken", { message: errorMessage });
      setLoadingInvitation(false);
    }
  };

  // 招待トークンが変更されたら招待情報を取得
  // URLパラメータから取得した場合は実行しない（重複を防ぐ）
  useEffect(() => {
    const urlToken = searchParams.get("token");
    if (
      invitationToken &&
      invitationToken.length > 0 &&
      invitationToken !== urlToken
    ) {
      loadInvitationInfo(invitationToken);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [invitationToken]);

  // パスワードの入力状態で新規ユーザーかどうかを判断
  // 初期状態では新規ユーザーとして扱い、パスワードフィールドを表示
  // 既存ユーザーの判定は、バックエンドのregister APIで行われる

  const onSubmit = async (data: RegisterFormData) => {
    try {
      const response = await authApi.register(data);
      if (response.success) {
        // 登録成功後、ログイン画面にリダイレクト
        router.push("/login?registered=true");
      } else {
        setError("root", { message: "ユーザー登録に失敗しました" });
      }
    } catch (err: any) {
      const errorMessage =
        err.response?.data?.error?.message || "ユーザー登録に失敗しました";
      setError("root", { message: errorMessage });
    }
  };

  if (loadingInvitation) {
    return (
      <div className="text-center py-8">
        <p className="text-gray-600">招待情報を読み込み中...</p>
      </div>
    );
  }

  return (
    <form className="mt-8 space-y-6" onSubmit={handleSubmit(onSubmit)}>
      {/* エラーメッセージ */}
      {errors.root && (
        <div className="text-xs bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {errors.root.message}
        </div>
      )}

      <div className="space-y-4">
        {/* 招待トークン */}
        <div>
          <label
            htmlFor="invitationToken"
            className="block text-sm font-medium text-gray-700"
          >
            招待トークン <span className="text-red-500">*</span>
          </label>
          <input
            id="invitationToken"
            type="text"
            {...register("invitationToken")}
            className={`mt-1 appearance-none relative block w-full px-3 py-2 border ${
              errors.invitationToken
                ? "border-red-300 focus:border-red-500 focus:ring-red-500"
                : "border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
            } placeholder-gray-500 text-gray-900 rounded-md focus:outline-none sm:text-sm`}
            placeholder="招待トークンを入力してください"
          />
          {errors.invitationToken && (
            <p className="mt-1 text-sm text-red-600">
              {errors.invitationToken.message}
            </p>
          )}
          {invitationInfo && (
            <p className="mt-1 text-sm text-green-600">
              招待情報を取得しました: {invitationInfo.companyName} (
              {invitationInfo.role})
            </p>
          )}
        </div>

        {/* メールアドレス */}
        <div>
          <label
            htmlFor="email"
            className="block text-sm font-medium text-gray-700"
          >
            メールアドレス <span className="text-red-500">*</span>
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            {...register("email")}
            disabled={!!invitationInfo}
            className={`mt-1 appearance-none relative block w-full px-3 py-2 border ${
              errors.email
                ? "border-red-300 focus:border-red-500 focus:ring-red-500"
                : "border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
            } placeholder-gray-500 text-gray-900 rounded-md focus:outline-none sm:text-sm ${
              invitationInfo ? "bg-gray-100" : ""
            }`}
            placeholder="example@example.com"
          />
          {errors.email && (
            <p className="mt-1 text-sm text-red-600">{errors.email.message}</p>
          )}
        </div>

        {/* パスワード */}
        <div>
          <label
            htmlFor="password"
            className="block text-sm font-medium text-gray-700"
          >
            パスワード <span className="text-red-500">*</span>
          </label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            {...register("password")}
            className={`mt-1 appearance-none relative block w-full px-3 py-2 border ${
              errors.password
                ? "border-red-300 focus:border-red-500 focus:ring-red-500"
                : "border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
            } placeholder-gray-500 text-gray-900 rounded-md focus:outline-none sm:text-sm`}
            placeholder="パスワード"
          />
          {errors.password && (
            <p className="mt-1 text-sm text-red-600">
              {errors.password.message}
            </p>
          )}
        </div>

        {/* 姓 */}
        <div>
          <label
            htmlFor="familyName"
            className="block text-sm font-medium text-gray-700"
          >
            姓 <span className="text-red-500">*</span>
          </label>
          <input
            id="familyName"
            type="text"
            autoComplete="family-name"
            {...register("familyName")}
            className={`mt-1 appearance-none relative block w-full px-3 py-2 border ${
              errors.familyName
                ? "border-red-300 focus:border-red-500 focus:ring-red-500"
                : "border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
            } placeholder-gray-500 text-gray-900 rounded-md focus:outline-none sm:text-sm`}
            placeholder="山田"
          />
          {errors.familyName && (
            <p className="mt-1 text-sm text-red-600">
              {errors.familyName.message}
            </p>
          )}
        </div>

        {/* 名 */}
        <div>
          <label
            htmlFor="firstName"
            className="block text-sm font-medium text-gray-700"
          >
            名 <span className="text-red-500">*</span>
          </label>
          <input
            id="firstName"
            type="text"
            autoComplete="given-name"
            {...register("firstName")}
            className={`mt-1 appearance-none relative block w-full px-3 py-2 border ${
              errors.firstName
                ? "border-red-300 focus:border-red-500 focus:ring-red-500"
                : "border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
            } placeholder-gray-500 text-gray-900 rounded-md focus:outline-none sm:text-sm`}
            placeholder="太郎"
          />
          {errors.firstName && (
            <p className="mt-1 text-sm text-red-600">
              {errors.firstName.message}
            </p>
          )}
        </div>

        {/* 姓（カナ） */}
        <div>
          <label
            htmlFor="familyNameKana"
            className="block text-sm font-medium text-gray-700"
          >
            姓（カナ）<span className="text-gray-400 text-xs">（任意）</span>
          </label>
          <input
            id="familyNameKana"
            type="text"
            autoComplete="family-name"
            {...register("familyNameKana")}
            className="mt-1 appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            placeholder="ヤマダ"
          />
        </div>

        {/* 名（カナ） */}
        <div>
          <label
            htmlFor="firstNameKana"
            className="block text-sm font-medium text-gray-700"
          >
            名（カナ）<span className="text-gray-400 text-xs">（任意）</span>
          </label>
          <input
            id="firstNameKana"
            type="text"
            autoComplete="given-name"
            {...register("firstNameKana")}
            className="mt-1 appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            placeholder="タロウ"
          />
        </div>
      </div>

      <div className="space-y-3">
        <Button
          type="submit"
          variant="default"
          disabled={isSubmitting}
          className="w-full"
        >
          {isSubmitting ? "登録中..." : "登録"}
        </Button>
        <Button
          type="button"
          variant="link"
          onClick={() => router.push("/login")}
          className="w-full"
        >
          既にアカウントをお持ちの方はこちら
        </Button>
      </div>
    </form>
  );
}
