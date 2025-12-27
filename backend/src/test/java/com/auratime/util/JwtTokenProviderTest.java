package com.auratime.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * JwtTokenProviderの単体テスト
 *
 * <p>
 * JWTトークンの生成、検証、情報抽出をテストするクラスです。
 * テスト用のシークレットキーを使用して、JWTトークンの動作を確認します。
 * </p>
 *
 * <h3>テスト対象</h3>
 * <ul>
 * <li>トークンの生成（generateToken）</li>
 * <li>トークンの検証（validateToken）</li>
 * <li>トークンからの情報抽出（getUserIdFromToken, getCompanyIdFromToken,
 * getRoleFromToken）</li>
 * </ul>
 *
 * <h3>テスト方針</h3>
 * <ul>
 * <li>正常系: 有効なトークンの生成・検証・情報抽出を確認</li>
 * <li>異常系: 無効なトークン、空のトークン、nullトークンの検証を確認</li>
 * </ul>
 *
 * @see com.auratime.util.JwtTokenProvider
 */
@DisplayName("JwtTokenProvider テスト")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    @SuppressWarnings("null")
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",
                "test-secret-key-for-jwt-token-validation-testing-purposes-only");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", 86400000L); // 24 hours
    }

    @Test
    @DisplayName("正常系：有効なトークンの生成と検証")
    void testGenerateAndValidateToken_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String role = "employee";

        // When
        String token = jwtTokenProvider.generateToken(userId, companyId, role);
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(token).isNotNull();
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("正常系：トークンからユーザーIDを取得")
    void testGetUserIdFromToken_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String role = "employee";

        String token = jwtTokenProvider.generateToken(userId, companyId, role);

        // When
        UUID extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("正常系：トークンから会社IDを取得")
    void testGetCompanyIdFromToken_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String role = "employee";

        String token = jwtTokenProvider.generateToken(userId, companyId, role);

        // When
        UUID extractedCompanyId = jwtTokenProvider.getCompanyIdFromToken(token);

        // Then
        assertThat(extractedCompanyId).isEqualTo(companyId);
    }

    @Test
    @DisplayName("正常系：トークンからロールを取得")
    void testGetRoleFromToken_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String role = "admin";

        String token = jwtTokenProvider.generateToken(userId, companyId, role);

        // When
        String extractedRole = jwtTokenProvider.getRoleFromToken(token);

        // Then
        assertThat(extractedRole).isEqualTo(role);
    }

    @Test
    @DisplayName("異常系：無効なトークン")
    void testValidateToken_InvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("異常系：空のトークン")
    void testValidateToken_EmptyToken() {
        // Given
        String emptyToken = "";

        // When
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("異常系：nullトークン")
    void testValidateToken_NullToken() {
        // Given
        String nullToken = null;

        // When
        boolean isValid = jwtTokenProvider.validateToken(nullToken);

        // Then
        assertThat(isValid).isFalse();
    }
}
