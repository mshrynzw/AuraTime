import { loginSchema, registerSchema } from '@/lib/validation/schemas';
import { describe, expect, it } from '@jest/globals';

describe('バリデーションスキーマ', () => {
  describe('loginSchema', () => {
    it('正常系：有効なログイン情報', () => {
      const validData = {
        email: 'test@example.com',
        password: 'password123',
      };

      const result = loginSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('異常系：メールアドレス未入力', () => {
      const invalidData = {
        password: 'password123',
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
        password: 'password123',
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

    it('異常系：パスワードが8文字未満', () => {
      const invalidData = {
        email: 'test@example.com',
        password: 'short',
      };

      const result = loginSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('8文字以上');
      }
    });
  });

  describe('registerSchema', () => {
    it('正常系：有効な登録情報', () => {
      const validData = {
        email: 'newuser@example.com',
        password: 'password123',
        familyName: '山田',
        firstName: '太郎',
      };

      const result = registerSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('正常系：カナ名も含む有効な登録情報', () => {
      const validData = {
        email: 'newuser@example.com',
        password: 'password123',
        familyName: '山田',
        firstName: '太郎',
        familyNameKana: 'ヤマダ',
        firstNameKana: 'タロウ',
      };

      const result = registerSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('異常系：姓未入力', () => {
      const invalidData = {
        email: 'newuser@example.com',
        password: 'password123',
        firstName: '太郎',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('姓は必須です');
      }
    });

    it('異常系：名未入力', () => {
      const invalidData = {
        email: 'newuser@example.com',
        password: 'password123',
        familyName: '山田',
      };

      const result = registerSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
      if (!result.success) {
        expect(result.error.errors[0].message).toContain('名は必須です');
      }
    });
  });
});



