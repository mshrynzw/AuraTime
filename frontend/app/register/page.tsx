"use client";

import { authApi } from "@/lib/api/auth";
import { registerSchema, type RegisterFormData } from "@/lib/validation/schemas";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";

export default function RegisterPage() {
  const router = useRouter();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    mode: "onBlur",
  });

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

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full space-y-8 p-8 bg-white rounded-lg shadow-md">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            AuraTime
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            新規ユーザー登録
          </p>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit(onSubmit)}>
          {/* エラーメッセージ */}
          {errors.root && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
              {errors.root.message}
            </div>
          )}

          <div className="space-y-4">
            {/* メールアドレス */}
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                メールアドレス
              </label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                {...register("email")}
                className={`mt-1 appearance-none relative block w-full px-3 py-2 border ${
                  errors.email
                    ? "border-red-300 focus:border-red-500 focus:ring-red-500"
                    : "border-gray-300 focus:ring-indigo-500 focus:border-indigo-500"
                } placeholder-gray-500 text-gray-900 rounded-md focus:outline-none sm:text-sm`}
                placeholder="example@example.com"
              />
              {errors.email && (
                <p className="mt-1 text-sm text-red-600">{errors.email.message}</p>
              )}
            </div>

            {/* パスワード */}
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                パスワード
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
                placeholder="8文字以上"
              />
              {errors.password && (
                <p className="mt-1 text-sm text-red-600">{errors.password.message}</p>
              )}
            </div>

            {/* 姓 */}
            <div>
              <label htmlFor="familyName" className="block text-sm font-medium text-gray-700">
                姓
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
                <p className="mt-1 text-sm text-red-600">{errors.familyName.message}</p>
              )}
            </div>

            {/* 名 */}
            <div>
              <label htmlFor="firstName" className="block text-sm font-medium text-gray-700">
                名
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
                <p className="mt-1 text-sm text-red-600">{errors.firstName.message}</p>
              )}
            </div>

            {/* 姓（カナ） */}
            <div>
              <label htmlFor="familyNameKana" className="block text-sm font-medium text-gray-700">
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
              <label htmlFor="firstNameKana" className="block text-sm font-medium text-gray-700">
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
            <button
              type="submit"
              disabled={isSubmitting}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? "登録中..." : "登録"}
            </button>
            <button
              type="button"
              onClick={() => router.push("/login")}
              className="w-full text-center text-sm text-indigo-600 hover:text-indigo-800"
            >
              既にアカウントをお持ちの方はこちら
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}



