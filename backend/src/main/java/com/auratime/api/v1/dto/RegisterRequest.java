package com.auratime.api.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ユーザー登録リクエストDTO
 *
 * <p>
 * ユーザー登録API（{@code POST /api/v1/auth/register}）のリクエストボディです。
 * </p>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@Data
public class RegisterRequest {
    /** メールアドレス（必須、有効なメールアドレス形式） */
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;

    /** パスワード（必須、8文字以上） */
    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, message = "パスワードは8文字以上である必要があります")
    private String password;

    /** 姓（必須） */
    @NotBlank(message = "姓は必須です")
    private String familyName;

    /** 名（必須） */
    @NotBlank(message = "名は必須です")
    private String firstName;

    /** 姓（カナ）（任意） */
    private String familyNameKana;

    /** 名（カナ）（任意） */
    private String firstNameKana;
}
