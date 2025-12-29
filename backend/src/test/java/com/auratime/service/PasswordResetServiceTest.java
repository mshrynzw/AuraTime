package com.auratime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.auratime.api.v1.dto.PasswordResetConfirmRequest;
import com.auratime.api.v1.dto.PasswordResetRequestRequest;
import com.auratime.domain.PasswordResetToken;
import com.auratime.domain.User;
import com.auratime.repository.PasswordResetTokenRepository;
import com.auratime.repository.UserRepository;

/**
 * PasswordResetServiceの単体テスト
 *
 * <p>
 * PasswordResetServiceのビジネスロジックをテストするクラスです。
 * Mockitoを使用して依存関係をモック化し、各メソッドの正常系・異常系をテストします。
 * </p>
 *
 * <h3>テスト対象</h3>
 * <ul>
 * <li>パスワードリセット要求（requestPasswordReset）</li>
 * <li>パスワードリセット実行（confirmPasswordReset）</li>
 * </ul>
 *
 * @see com.auratime.service.PasswordResetService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService テスト")
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private UUID userId;
    private User testUser;
    private PasswordResetToken testToken;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("$2a$10$oldHashedPassword")
                .familyName("山田")
                .firstName("太郎")
                .status("active")
                .build();

        testToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token("reset-token-123")
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("正常系：パスワードリセット要求")
    @SuppressWarnings("null")
    void testRequestPasswordReset_Success() {
        // Given
        PasswordResetRequestRequest request = new PasswordResetRequestRequest();
        request.setEmail("test@example.com");

        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId(UUID.randomUUID());
            }
            return token;
        });

        // When
        passwordResetService.requestPasswordReset(request);

        // Then
        verify(userRepository).findByEmailAndDeletedAtIsNull("test@example.com");
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("正常系：存在しないユーザーでも成功（セキュリティ上の理由）")
    void testRequestPasswordReset_UserNotFound() {
        // Given
        PasswordResetRequestRequest request = new PasswordResetRequestRequest();
        request.setEmail("notfound@example.com");

        when(userRepository.findByEmailAndDeletedAtIsNull("notfound@example.com"))
                .thenReturn(Optional.empty());

        // When
        passwordResetService.requestPasswordReset(request);

        // Then
        verify(userRepository).findByEmailAndDeletedAtIsNull("notfound@example.com");
        verify(tokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("正常系：パスワードリセット実行")
    @SuppressWarnings("null")
    void testConfirmPasswordReset_Success() {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken("reset-token-123");
        request.setNewPassword("NewPassword123!");

        when(tokenRepository.findByTokenAndUsedAtIsNull("reset-token-123"))
                .thenReturn(Optional.of(testToken));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("$2a$10$newHashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            return token;
        });

        // When
        passwordResetService.confirmPasswordReset(request);

        // Then
        assertThat(testUser.getPasswordHash()).isEqualTo("$2a$10$newHashedPassword");
        assertThat(testToken.getUsedAt()).isNotNull();
        verify(tokenRepository).findByTokenAndUsedAtIsNull("reset-token-123");
        verify(passwordEncoder).encode("NewPassword123!");
        verify(userRepository).save(testUser);
        verify(tokenRepository).save(testToken);
    }

    @Test
    @DisplayName("異常系：トークンが見つからない")
    void testConfirmPasswordReset_TokenNotFound() {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken("invalid-token");
        request.setNewPassword("NewPassword123!");

        when(tokenRepository.findByTokenAndUsedAtIsNull("invalid-token"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> passwordResetService.confirmPasswordReset(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode().value()).isEqualTo(404);
                    assertThat(ex.getReason()).contains("リセットトークンが見つかりません");
                });

        verify(tokenRepository).findByTokenAndUsedAtIsNull("invalid-token");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("異常系：トークンが期限切れ")
    void testConfirmPasswordReset_TokenExpired() {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken("expired-token");
        request.setNewPassword("NewPassword123!");

        testToken.setExpiresAt(OffsetDateTime.now().minusHours(1));

        when(tokenRepository.findByTokenAndUsedAtIsNull("expired-token"))
                .thenReturn(Optional.of(testToken));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.confirmPasswordReset(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode().value()).isEqualTo(400);
                    assertThat(ex.getReason()).contains("リセットトークンが無効または期限切れです");
                });

        verify(tokenRepository).findByTokenAndUsedAtIsNull("expired-token");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("異常系：トークンが既に使用済み")
    void testConfirmPasswordReset_TokenAlreadyUsed() {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken("used-token");
        request.setNewPassword("NewPassword123!");

        testToken.setUsedAt(OffsetDateTime.now().minusHours(1));

        when(tokenRepository.findByTokenAndUsedAtIsNull("used-token"))
                .thenReturn(Optional.empty()); // 使用済みトークンは検索されない

        // When & Then
        assertThatThrownBy(() -> passwordResetService.confirmPasswordReset(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode().value()).isEqualTo(404);
                });

        verify(tokenRepository).findByTokenAndUsedAtIsNull("used-token");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}

