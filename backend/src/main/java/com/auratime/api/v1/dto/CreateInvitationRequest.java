package com.auratime.api.v1.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 招待作成リクエストDTO
 *
 * <p>
 * 招待作成API（{@code POST /api/v1/invitations}）のリクエストボディです。
 * </p>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@Data
public class CreateInvitationRequest {
    /** 招待先メールアドレス（必須、有効なメールアドレス形式） */
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;

    /** 付与するロール（必須） */
    @NotBlank(message = "ロールは必須です")
    @Pattern(regexp = "system_admin|admin|manager|employee", message = "有効なロールを指定してください")
    private String role;

    /** 社員番号（必須） */
    @NotBlank(message = "社員番号は必須です")
    private String employeeNo;

    /** 雇用区分（任意） */
    @Pattern(regexp = "fulltime|parttime|contract", message = "有効な雇用区分を指定してください")
    private String employmentType;

    /** 入社日（任意） */
    private LocalDate hireDate;

    /** 有効期限（日数、デフォルト7日） */
    private Integer expiresInDays;
}

