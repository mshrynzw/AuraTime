package com.auratime.api.v1.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ログインレスポンスDTO
 *
 * <p>
 * ログインAPI（{@code POST /api/v1/auth/login}）のレスポンスです。
 * JWTトークンとユーザー基本情報を含みます。
 * </p>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    /** JWTトークン（認証に使用） */
    private String token;

    /** ユーザー基本情報 */
    private UserInfo user;

    /**
     * ユーザー基本情報
     *
     * <p>
     * ログインしたユーザーの基本情報を保持します。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        /** ユーザーID */
        private UUID id;

        /** メールアドレス */
        private String email;

        /** 姓 */
        private String familyName;

        /** 名 */
        private String firstName;

        /** 会社ID（マルチテナント用） */
        private UUID companyId;

        /** ロール（system_admin, admin, manager, employee） */
        private String role;
    }
}
