package com.auratime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.auratime.api.v1.dto.LoginRequest;
import com.auratime.api.v1.dto.LoginResponse;
import com.auratime.api.v1.dto.RegisterRequest;
import com.auratime.domain.CompanyMembership;
import com.auratime.domain.User;
import com.auratime.repository.CompanyMembershipRepository;
import com.auratime.repository.CompanyRepository;
import com.auratime.repository.UserRepository;
import com.auratime.util.JwtTokenProvider;

/**
 * AuthServiceの単体テスト
 *
 * <p>
 * AuthServiceのビジネスロジックをテストするクラスです。
 * Mockitoを使用して依存関係をモック化し、各メソッドの正常系・異常系をテストします。
 * </p>
 *
 * <h3>テスト対象</h3>
 * <ul>
 * <li>ユーザー登録（register）</li>
 * <li>ログイン（login）</li>
 * </ul>
 *
 * <h3>テスト方針</h3>
 * <ul>
 * <li>正常系: 期待通りの動作を確認</li>
 * <li>異常系: 適切な例外がスローされることを確認</li>
 * <li>モックの検証: 期待されるメソッドが呼び出されることを確認</li>
 * </ul>
 *
 * @see com.auratime.service.AuthService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService テスト")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMembershipRepository companyMembershipRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private UUID companyId;
    private User testUser;
    private CompanyMembership testMembership;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .familyName("山田")
                .firstName("太郎")
                .status("active")
                .build();

        testMembership = CompanyMembership.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(userId)
                .role("employee")
                .build();
    }

    @Test
    @DisplayName("正常系：新規ユーザー登録")
    @SuppressWarnings("null")
    void testRegister_Success() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setFamilyName("佐藤");
        request.setFirstName("花子");

        // システムボットのモック設定
        UUID systemBotId = UUID.randomUUID();
        User systemBot = User.builder()
                .id(systemBotId)
                .email("system-bot@auratime.com")
                .passwordHash("dummy")
                .familyName("System")
                .firstName("Bot")
                .status("active")
                .build();

        // 新規ユーザーの検索時は空を返す
        when(userRepository.findByEmailAndDeletedAtIsNull("newuser@example.com"))
                .thenReturn(Optional.empty());
        // システムボットの検索時はシステムボットを返す
        when(userRepository.findByEmailAndDeletedAtIsNull("system-bot@auratime.com"))
                .thenReturn(Optional.of(systemBot));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            @SuppressWarnings("null")
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID());
            }
            return user;
        });

        // When
        User result = authService.register(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("newuser@example.com");
        assertThat(result.getFamilyName()).isEqualTo("佐藤");
        assertThat(result.getFirstName()).isEqualTo("花子");
        assertThat(result.getStatus()).isEqualTo("active");
        verify(userRepository).findByEmailAndDeletedAtIsNull("newuser@example.com");
        verify(userRepository).findByEmailAndDeletedAtIsNull("system-bot@auratime.com");
        verify(passwordEncoder).encode("password123");
        // システムボットの検索と新規ユーザーの保存でsaveが呼ばれる
        verify(userRepository, org.mockito.Mockito.atLeastOnce()).save(any(User.class));
    }

    @Test
    @DisplayName("異常系：重複メールアドレス")
    @SuppressWarnings("null")
    void testRegister_DuplicateEmail() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setFamilyName("佐藤");
        request.setFirstName("花子");

        when(userRepository.findByEmailAndDeletedAtIsNull("existing@example.com"))
                .thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("このメールアドレスは既に登録されています");

        verify(userRepository).findByEmailAndDeletedAtIsNull("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("正常系：正しい認証情報でログイン")
    void testLogin_Success() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        String token = "test-jwt-token";

        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
        when(companyMembershipRepository.findByUserIdAndDeletedAtIsNull(userId))
                .thenReturn(List.of(testMembership));
        when(jwtTokenProvider.generateToken(userId, companyId, "employee")).thenReturn(token);

        // When
        LoginResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(token);
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUser().getCompanyId()).isEqualTo(companyId);
        assertThat(response.getUser().getRole()).isEqualTo("employee");

        verify(userRepository).findByEmailAndDeletedAtIsNull("test@example.com");
        verify(passwordEncoder).matches("password123", "$2a$10$hashedPassword");
        verify(jwtTokenProvider).generateToken(userId, companyId, "employee");
    }

    @Test
    @DisplayName("異常系：存在しないユーザー")
    void testLogin_UserNotFound() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("notfound@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmailAndDeletedAtIsNull("notfound@example.com"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("メールアドレスまたはパスワードが正しくありません");

        verify(userRepository).findByEmailAndDeletedAtIsNull("notfound@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("異常系：間違ったパスワード")
    void testLogin_WrongPassword() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("メールアドレスまたはパスワードが正しくありません");

        verify(userRepository).findByEmailAndDeletedAtIsNull("test@example.com");
        verify(passwordEncoder).matches("wrongpassword", "$2a$10$hashedPassword");
        verify(jwtTokenProvider, never()).generateToken(any(), any(), anyString());
    }

    @Test
    @DisplayName("異常系：無効化されたアカウント")
    void testLogin_InactiveAccount() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User inactiveUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .status("inactive")
                .build();

        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("このアカウントは無効化されています");

        verify(userRepository).findByEmailAndDeletedAtIsNull("test@example.com");
        verify(passwordEncoder).matches("password123", "$2a$10$hashedPassword");
        verify(jwtTokenProvider, never()).generateToken(any(), any(), anyString());
    }

    @Test
    @DisplayName("異常系：会社に所属していないユーザー")
    void testLogin_NoCompanyMembership() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
        when(companyMembershipRepository.findByUserIdAndDeletedAtIsNull(userId))
                .thenReturn(List.of());

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ユーザーはどの会社にも所属していません");

        verify(userRepository).findByEmailAndDeletedAtIsNull("test@example.com");
        verify(passwordEncoder).matches("password123", "$2a$10$hashedPassword");
        verify(companyMembershipRepository).findByUserIdAndDeletedAtIsNull(userId);
        verify(jwtTokenProvider, never()).generateToken(any(), any(), anyString());
    }
}
