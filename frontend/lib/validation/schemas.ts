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
    .min(12, "パスワードは12文字以上である必要があります")
    .regex(
      /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{12,}$/,
      "パスワードは数字・大文字・小文字・記号を含む12文字以上である必要があります"
    ),
});

export type LoginFormData = z.infer<typeof loginSchema>;

// ユーザー登録スキーマ（招待トークン必須）
// 既存ユーザーと新規ユーザーでバリデーションが異なるため、superRefineを使用
export const registerSchema = z
  .object({
    invitationToken: z
      .string({ required_error: "招待トークンは必須です" })
      .min(1, "招待トークンは必須です"),
    email: z
      .string({ required_error: "メールアドレスは必須です" })
      .min(1, "メールアドレスは必須です")
      .email("有効なメールアドレスを入力してください"),
    password: z.string().optional(),
    familyName: z.string().optional(),
    firstName: z.string().optional(),
    familyNameKana: z.string().optional(),
    firstNameKana: z.string().optional(),
  })
  .superRefine((data, ctx) => {
    // 新規ユーザーの場合、パスワード、氏名は必須
    // 既存ユーザーの場合はバックエンドで判定するため、フロントエンドでは簡易チェック
    // パスワードが入力されている場合は新規ユーザーとみなす
    // パスワードが空文字列または未定義の場合は、新規ユーザーとして扱い、必須チェックを行う
    const hasPassword = data.password !== undefined && data.password.length > 0;
    const isNewUser = hasPassword; // パスワードが入力されている場合は新規ユーザー

    if (isNewUser) {
      // 新規ユーザーの場合
      const passwordPattern =
        /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{12,}$/;
      if (!data.password || data.password.length < 12) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "パスワードは12文字以上である必要があります",
          path: ["password"],
        });
      } else if (!passwordPattern.test(data.password)) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "パスワードは数字・大文字・小文字・記号を含む必要があります",
          path: ["password"],
        });
      }
      if (!data.familyName || data.familyName.length === 0) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "姓は必須です",
          path: ["familyName"],
        });
      }
      if (!data.firstName || data.firstName.length === 0) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          message: "名は必須です",
          path: ["firstName"],
        });
      }
    }
    // パスワードが未入力の場合、既存ユーザーの可能性があるため、フロントエンドではチェックしない
    // バックエンドで既存ユーザーかどうかを判定する
    // 既存ユーザーの場合、パスワードと氏名は不要（バックエンドでチェック）
  });

export type RegisterFormData = z.infer<typeof registerSchema>;

// パスワードリセット要求スキーマ
export const passwordResetRequestSchema = z.object({
  email: z
    .string({ required_error: "メールアドレスは必須です" })
    .min(1, "メールアドレスは必須です")
    .email("有効なメールアドレスを入力してください"),
});

export type PasswordResetRequestFormData = z.infer<
  typeof passwordResetRequestSchema
>;

// パスワードリセット実行スキーマ
export const passwordResetConfirmSchema = z.object({
  token: z
    .string({ required_error: "リセットトークンは必須です" })
    .min(1, "リセットトークンは必須です"),
  newPassword: z
    .string({ required_error: "パスワードは必須です" })
    .min(1, "パスワードは必須です")
    .min(12, "パスワードは12文字以上である必要があります")
    .regex(
      /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{12,}$/,
      "パスワードは数字・大文字・小文字・記号を含む12文字以上である必要があります"
    ),
});

export type PasswordResetConfirmFormData = z.infer<
  typeof passwordResetConfirmSchema
>;
