package com.auratime.api.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * パスワードリセット実行リクエストDTO
 *
 * <p>
 * パスワードリセット実行API（{@code POST /api/v1/auth/password-reset/confirm}）のリクエストボディです。
 * </p>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@Data
public class PasswordResetConfirmRequest {
    /** リセットトークン（必須） */
    @NotBlank(message = "リセットトークンは必須です")
    private String token;

    /** 新しいパスワード（必須、数字・大文字・小文字・記号を含む12文字以上） */
    @NotBlank(message = "パスワードは必須です")
    @Size(min = 12, message = "パスワードは12文字以上である必要があります")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{12,}$",
            message = "パスワードは数字・大文字・小文字・記号を含む12文字以上である必要があります"
    )
    private String newPassword;
}

