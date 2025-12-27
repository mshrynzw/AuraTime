import { z } from "zod";

// ログインスキーマ
export const loginSchema = z.object({
  email: z
    .string({ required_error: "メールアドレスは必須です" })
    .min(1, "メールアドレスは必須です")
    .email("有効なメールアドレスを入力してください"),
  password: z
    .string({ required_error: "パスワードは必須です" })
    .min(1, "パスワードは必須です")
    .min(8, "パスワードは8文字以上である必要があります"),
});

export type LoginFormData = z.infer<typeof loginSchema>;

// ユーザー登録スキーマ
export const registerSchema = z.object({
  email: z
    .string({ required_error: "メールアドレスは必須です" })
    .min(1, "メールアドレスは必須です")
    .email("有効なメールアドレスを入力してください"),
  password: z
    .string({ required_error: "パスワードは必須です" })
    .min(1, "パスワードは必須です")
    .min(8, "パスワードは8文字以上である必要があります"),
  familyName: z.string({ required_error: "姓は必須です" }).min(1, "姓は必須です"),
  firstName: z.string({ required_error: "名は必須です" }).min(1, "名は必須です"),
  familyNameKana: z.string().optional(),
  firstNameKana: z.string().optional(),
});

export type RegisterFormData = z.infer<typeof registerSchema>;



