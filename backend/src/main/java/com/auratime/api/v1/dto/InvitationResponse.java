package com.auratime.api.v1.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Data;

/**
 * 招待情報レスポンスDTO
 *
 * <p>
 * 招待情報取得API（{@code GET /api/v1/auth/invitations/{token}}）のレスポンスボディです。
 * </p>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@Data
public class InvitationResponse {
    /** 招待ID */
    private UUID id;

    /** 会社ID */
    private UUID companyId;

    /** 会社名 */
    private String companyName;

    /** 招待先メールアドレス */
    private String email;

    /** 付与するロール */
    private String role;

    /** 社員番号 */
    private String employeeNo;

    /** 雇用区分 */
    private String employmentType;

    /** 入社日 */
    private java.time.LocalDate hireDate;

    /** 有効期限 */
    private OffsetDateTime expiresAt;
}

