package com.auratime.api.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ユーザー登録リクエストDTO
 *
 * <p>
 * ユーザー登録API（{@code POST /api/v1/auth/register}）のリクエストボディです。
 * 招待トークンが必須となりました。
 * </p>
 *
 * <h3>既存ユーザーの場合</h3>
 * <p>
 * メールアドレスが既に登録されている場合、パスワードは不要です。
 * 社員番号のみ入力してください。
 * </p>
 *
 * <h3>新規ユーザーの場合</h3>
 * <p>
 * メールアドレスが未登録の場合、パスワード、氏名、カナを入力してください。
 * </p>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@Data
public class RegisterRequest {
    /** 招待トークン（必須） */
    @NotBlank(message = "招待トークンは必須です")
    private String invitationToken;

    /** メールアドレス（必須、有効なメールアドレス形式） */
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;

    /** パスワード（新規ユーザーの場合必須、数字・大文字・小文字・記号を含む12文字以上） */
    @Size(min = 12, message = "パスワードは12文字以上である必要があります")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{12,}$",
            message = "パスワードは数字・大文字・小文字・記号を含む12文字以上である必要があります"
    )
    private String password;

    /** 姓（新規ユーザーの場合必須） */
    private String familyName;

    /** 名（新規ユーザーの場合必須） */
    private String firstName;

    /** 姓（カナ）（任意） */
    private String familyNameKana;

    /** 名（カナ）（任意） */
    private String firstNameKana;
}
