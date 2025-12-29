import {
  loginSchema,
  passwordResetConfirmSchema,
  passwordResetRequestSchema,
  registerSchema,
} from '@/lib/validation/schemas';

describe('バリデーションスキーマ', () => {
  describe('loginSchema', () => {
    it('正常系：有効なログイン情報', () => {
      const validData = {
        email: 'test@example.com',
        password: 'Password123!@#',
      };

      const result = loginSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('異常系：メールアドレス未入力', () => {
      const invalidData = {
        password: 'Password123!@#',
      };

      const result = loginSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('メールアドレスは必須です');
      }
    });

    it('異常系：無効なメールアドレス形式', () => {
      const invalidData = {
        email: 'invalid-email',
        password: 'Password123!@#',
      };

      const result = loginSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('有効なメールアドレス');
      }
    });

    it('異常系：パスワード未入力', () => {
      const invalidData = {
        email: 'test@example.com',
      };

      const result = loginSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('パスワードは必須です');
      }
    });

    it('異常系：パスワードが12文字未満', () => {
      const invalidData = {
        email: 'test@example.com',
        password: 'Short1!',
      };

      const result = loginSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('12文字以上');
      }
    });

    it('異常系：パスワードに数字が含まれていない', () => {
      const invalidData = {
        email: 'test@example.com',
        password: 'PasswordOnly!@#',
      };

      const result = loginSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('数字・大文字・小文字・記号を含む');
      }
    });

    it('異常系：パスワードに大文字が含まれていない', () => {
      const invalidData = {
        email: 'test@example.com',
        password: 'password123!@#',
      };

      const result = loginSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('数字・大文字・小文字・記号を含む');
      }
    });

    it('異常系：パスワードに小文字が含まれていない', () => {
      const invalidData = {
        email: 'test@example.com',
        password: 'PASSWORD123!@#',
      };

      const result = loginSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('数字・大文字・小文字・記号を含む');
      }
    });

    it('異常系：パスワードに記号が含まれていない', () => {
      const invalidData = {
        email: 'test@example.com',
        password: 'Password123',
      };

      const result = loginSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        // 12文字以上なので、記号のエラーメッセージが表示される
        const errorMessages = result.error.errors.map(e => e.message);
        expect(errorMessages.some(msg => msg.includes('数字・大文字・小文字・記号を含む'))).toBe(true);
      }
    });
  });

  describe('registerSchema', () => {
    it('正常系：有効な登録情報（新規ユーザー）', () => {
      const validData = {
        invitationToken: 'test-token',
        email: 'newuser@example.com',
        password: 'NewPassword123!@#',
        familyName: '山田',
        firstName: '太郎',
      };

      const result = registerSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('正常系：カナ名も含む有効な登録情報（新規ユーザー）', () => {
      const validData = {
        invitationToken: 'test-token',
        email: 'newuser@example.com',
        password: 'NewPassword123!@#',
        familyName: '山田',
        firstName: '太郎',
        familyNameKana: 'ヤマダ',
        firstNameKana: 'タロウ',
      };

      const result = registerSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('正常系：有効な登録情報（既存ユーザー - パスワードなし）', () => {
      const validData = {
        invitationToken: 'test-token',
        email: 'existing@example.com',
        // パスワードが未定義または空の場合は既存ユーザーとみなされる
      };

      const result = registerSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('異常系：招待トークン未入力', () => {
      const invalidData = {
        email: 'newuser@example.com',
        password: 'NewPassword123!@#',
        familyName: '山田',
        firstName: '太郎',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('招待トークンは必須です');
      }
    });

    it('異常系：メールアドレス未入力（新規ユーザー）', () => {
      const invalidData = {
        invitationToken: 'test-token',
        password: 'NewPassword123!@#',
        familyName: '山田',
        firstName: '太郎',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('メールアドレスは必須です');
      }
    });

    it('異常系：パスワード未入力（新規ユーザー - パスワードが空文字列）', () => {
      const invalidData = {
        invitationToken: 'test-token',
        email: 'newuser@example.com',
        password: '', // 空文字列の場合は新規ユーザーとして扱われない
        familyName: '山田',
        firstName: '太郎',
      };

      // パスワードが空文字列の場合は既存ユーザーとみなされるため、バリデーションは通る
      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(true);
    });

    it('異常系：パスワードが12文字未満（新規ユーザー）', () => {
      const invalidData = {
        invitationToken: 'test-token',
        email: 'newuser@example.com',
        password: 'Short1!',
        familyName: '山田',
        firstName: '太郎',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        const passwordError = result.error.errors.find(
          (e) => e.path[0] === 'password'
        );
        expect(passwordError?.message).toContain('12文字以上');
      }
    });

    it('異常系：パスワードに数字・大文字・小文字・記号が含まれていない（新規ユーザー）', () => {
      const invalidData = {
        invitationToken: 'test-token',
        email: 'newuser@example.com',
        password: 'passwordonly',
        familyName: '山田',
        firstName: '太郎',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        const passwordError = result.error.errors.find(
          (e) => e.path[0] === 'password'
        );
        expect(passwordError?.message).toContain('数字・大文字・小文字・記号を含む');
      }
    });

    it('異常系：姓未入力（新規ユーザー）', () => {
      const invalidData = {
        invitationToken: 'test-token',
        email: 'newuser@example.com',
        password: 'NewPassword123!@#',
        firstName: '太郎',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        const familyNameError = result.error.errors.find(
          (e) => e.path[0] === 'familyName'
        );
        expect(familyNameError?.message).toContain('姓は必須です');
      }
    });

    it('異常系：名未入力（新規ユーザー）', () => {
      const invalidData = {
        invitationToken: 'test-token',
        email: 'newuser@example.com',
        password: 'NewPassword123!@#',
        familyName: '山田',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        const firstNameError = result.error.errors.find(
          (e) => e.path[0] === 'firstName'
        );
        expect(firstNameError?.message).toContain('名は必須です');
      }
    });
  });

  describe('passwordResetRequestSchema', () => {
    it('正常系：有効なパスワードリセット要求情報', () => {
      const validData = {
        email: 'test@example.com',
      };

      const result = passwordResetRequestSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('異常系：メールアドレス未入力', () => {
      const invalidData = {};

      const result = passwordResetRequestSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('メールアドレスは必須です');
      }
    });

    it('異常系：無効なメールアドレス形式', () => {
      const invalidData = {
        email: 'invalid-email',
      };

      const result = passwordResetRequestSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('有効なメールアドレス');
      }
    });
  });

  describe('passwordResetConfirmSchema', () => {
    it('正常系：有効なパスワードリセット実行情報', () => {
      const validData = {
        token: 'test-token',
        newPassword: 'NewPassword123!@#',
      };

      const result = passwordResetConfirmSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('異常系：トークン未入力', () => {
      const invalidData = {
        newPassword: 'NewPassword123!@#',
      };

      const result = passwordResetConfirmSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('トークンは必須です');
      }
    });

    it('異常系：パスワード未入力', () => {
      const invalidData = {
        token: 'test-token',
      };

      const result = passwordResetConfirmSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('パスワードは必須です');
      }
    });

    it('異常系：パスワードが12文字未満', () => {
      const invalidData = {
        token: 'test-token',
        newPassword: 'Short1!',
      };

      const result = passwordResetConfirmSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('12文字以上');
      }
    });

    it('異常系：パスワードに数字・大文字・小文字・記号が含まれていない', () => {
      const invalidData = {
        token: 'test-token',
        newPassword: 'passwordonly',
      };

      const result = passwordResetConfirmSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('数字・大文字・小文字・記号を含む');
      }
    });
  });
});




