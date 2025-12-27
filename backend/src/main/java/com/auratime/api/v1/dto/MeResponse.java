package com.auratime.api.v1.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 現在のユーザー情報レスポンスDTO
 *
 * <p>
 * 現在のユーザー情報取得API（{@code GET /api/v1/auth/me}）のレスポンスです。
 * ログイン中のユーザーの詳細情報を含みます。
 * </p>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeResponse {
    /** ユーザーID */
    private UUID id;

    /** メールアドレス */
    private String email;

    /** 姓 */
    private String familyName;

    /** 名 */
    private String firstName;

    /** 姓（カナ） */
    private String familyNameKana;

    /** 名（カナ） */
    private String firstNameKana;

    /** ステータス（active, inactive など） */
    private String status;

    /** 会社ID（マルチテナント用） */
    private UUID companyId;

    /** ロール（system_admin, admin, manager, employee） */
    private String role;
}
