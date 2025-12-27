package com.auratime.api.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ログインリクエストDTO
 *
 * <p>
 * ログインAPI（{@code POST /api/v1/auth/login}）のリクエストボディです。
 * </p>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@Data
public class LoginRequest {
    /** メールアドレス（必須、有効なメールアドレス形式） */
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;

    /** パスワード（必須） */
    @NotBlank(message = "パスワードは必須です")
    private String password;
}
