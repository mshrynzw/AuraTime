package com.auratime.api.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * パスワードリセット要求リクエストDTO
 *
 * <p>
 * パスワードリセット要求API（{@code POST /api/v1/auth/password-reset/request}）のリクエストボディです。
 * </p>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@Data
public class PasswordResetRequestRequest {
    /** メールアドレス（必須、有効なメールアドレス形式） */
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;
}

