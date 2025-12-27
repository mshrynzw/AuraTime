package com.auratime.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.auratime.api.v1.dto.LoginRequest;
import com.auratime.api.v1.dto.LoginResponse;
import com.auratime.api.v1.dto.RegisterRequest;
import com.auratime.domain.Company;
import com.auratime.domain.CompanyMembership;
import com.auratime.domain.User;
import com.auratime.repository.CompanyMembershipRepository;
import com.auratime.repository.CompanyRepository;
import com.auratime.repository.UserRepository;
import com.auratime.util.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 認証機能の統合テスト
 *
 * <p>
 * 認証機能全体の統合テストを行うクラスです。
 *
 * @SpringBootTestを使用して、アプリケーション全体を起動し、
 *                                      コントローラー、サービス、リポジトリ、フィルターが連携して動作することを確認します。
 *                                      </p>
 *
 *                                      <h3>テスト対象</h3>
 *                                      <ul>
 *                                      <li>ユーザー登録 → ログイン → ユーザー情報取得の一連フロー</li>
 *                                      <li>ログイン機能（正常系・異常系）</li>
 *                                      <li>JWTトークン認証（有効・無効・なし）</li>
 *                                      </ul>
 *
 *                                      <h3>テスト環境</h3>
 *                                      <ul>
 *                                      <li>データベース: H2（インメモリ）</li>
 *                                      <li>プロファイル:
 *                                      test（application-test.ymlを使用）</li>
 *                                      <li>トランザクション:
 *                                      各テスト後にロールバック（@Transactional）</li>
 *                                      </ul>
 *
 *                                      <h3>テスト方針</h3>
 *                                      <ul>
 *                                      <li>エンドツーエンドのフローをテスト</li>
 *                                      <li>実際のデータベース操作を確認</li>
 *                                      <li>JWTトークンの生成と検証を確認</li>
 *                                      <li>HTTPステータスコードとレスポンス形式を確認</li>
 *                                      </ul>
 *
 * @see com.auratime.api.v1.controller.AuthController
 * @see com.auratime.service.AuthService
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("認証統合テスト")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyMembershipRepository companyMembershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    private UUID systemBotUserId;
    private UUID companyId;
    private User testUser;
    private Company testCompany;

    @BeforeEach
    @SuppressWarnings("null")
    void setUp() {
        // システムボットユーザーを作成（created_by用）
        systemBotUserId = UUID.randomUUID();
        User systemBot = User.builder()
                .id(systemBotUserId)
                .email("system-bot@auratime.com")
                .passwordHash(passwordEncoder.encode("dummy"))
                .familyName("System")
                .firstName("Bot")
                .status("active")
                .createdBy(systemBotUserId)
                .updatedBy(systemBotUserId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        userRepository.save(systemBot);

        // テスト会社を作成
        companyId = UUID.randomUUID();
        testCompany = Company.builder()
                .id(companyId)
                .name("テスト会社")
                .code("TEST001")
                .createdBy(systemBotUserId)
                .updatedBy(systemBotUserId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        companyRepository.save(testCompany);

        // テストユーザーを作成
        UUID userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .familyName("山田")
                .firstName("太郎")
                .status("active")
                .createdBy(systemBotUserId)
                .updatedBy(systemBotUserId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        userRepository.save(testUser);

        // 会社メンバーシップを作成
        CompanyMembership membership = CompanyMembership.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(userId)
                .role("employee")
                .createdBy(systemBotUserId)
                .updatedBy(systemBotUserId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        companyMembershipRepository.save(membership);

        // データベースに確実に保存するため、flush()を呼び出す
        // @Transactionalの影響で、setUpで作成したデータがloginメソッドで見つからない場合があるため
        entityManager.flush();
    }

    @Test
    @DisplayName("正常系：ユーザー登録 → ログイン → /me取得の一連フロー")
    @SuppressWarnings("null")
    void testRegister_Login_GetMe_Success() throws Exception {
        // 1. ユーザー登録
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFamilyName("佐藤");
        registerRequest.setFirstName("花子");

        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // 2. ログイン
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("newuser@example.com");
        loginRequest.setPassword("password123");

        // 会社メンバーシップを作成（登録したユーザー用）
        User newUser = userRepository.findByEmailAndDeletedAtIsNull("newuser@example.com")
                .orElseThrow();
        CompanyMembership newMembership = CompanyMembership.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(newUser.getId())
                .role("employee")
                .createdBy(systemBotUserId)
                .updatedBy(systemBotUserId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        companyMembershipRepository.save(newMembership);

        String responseJson = mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // トークンを取得
        LoginResponse loginResponse = objectMapper.readValue(
                objectMapper.readTree(responseJson).get("data").toString(),
                LoginResponse.class);
        String token = loginResponse.getToken();

        // 3. /me エンドポイントでユーザー情報を取得
        // 注意: CompanyContextはJwtAuthenticationFilterで自動設定されるため、テストで事前設定する必要はない
        mockMvc.perform(get("/v1/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.familyName").value("佐藤"))
                .andExpect(jsonPath("$.data.firstName").value("花子"));
    }

    @Test
    @DisplayName("正常系：正しい認証情報でログイン")
    @SuppressWarnings("null")
    void testLogin_Success() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        // 会社メンバーシップが存在することを確認（setUpで作成済み）
        // 念のため、再度保存してflush()を呼び出す
        User user = userRepository.findByEmailAndDeletedAtIsNull("test@example.com")
                .orElseThrow();
        List<CompanyMembership> existingMemberships = companyMembershipRepository
                .findByUserIdAndDeletedAtIsNull(user.getId());

        if (existingMemberships.isEmpty()) {
            // 会社メンバーシップが存在しない場合は作成
            CompanyMembership membership = CompanyMembership.builder()
                    .id(UUID.randomUUID())
                    .companyId(companyId)
                    .userId(user.getId())
                    .role("employee")
                    .createdBy(systemBotUserId)
                    .updatedBy(systemBotUserId)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            companyMembershipRepository.save(membership);
        }

        entityManager.flush();
        entityManager.clear(); // キャッシュをクリアして、データベースから再読み込み

        // When & Then
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("異常系：間違った認証情報")
    @SuppressWarnings("null")
    void testLogin_WrongCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        // When & Then
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_CREDENTIALS"));
    }

    @Test
    @DisplayName("正常系：有効なトークンでアクセス")
    @SuppressWarnings("null")
    void testGetMe_ValidToken() throws Exception {
        // Given
        UUID userId = testUser.getId();

        // 会社メンバーシップが存在することを確認（setUpで作成済み）
        var existingMembership = companyMembershipRepository
                .findByUserIdAndCompanyIdAndDeletedAtIsNull(userId, companyId);

        if (existingMembership.isEmpty()) {
            // 会社メンバーシップが存在しない場合は作成
            CompanyMembership membership = CompanyMembership.builder()
                    .id(UUID.randomUUID())
                    .companyId(companyId)
                    .userId(userId)
                    .role("employee")
                    .createdBy(systemBotUserId)
                    .updatedBy(systemBotUserId)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            companyMembershipRepository.save(membership);
        }

        entityManager.flush();
        entityManager.clear(); // キャッシュをクリアして、データベースから再読み込み

        String token = jwtTokenProvider.generateToken(userId, companyId, "employee");
        // 注意: CompanyContextはJwtAuthenticationFilterで自動設定されるため、テストで事前設定する必要はない

        // When & Then
        var result = mockMvc.perform(get("/v1/auth/me")
                .header("Authorization", "Bearer " + token))
                .andReturn();

        // デバッグ用: エラーレスポンスを確認
        if (result.getResponse().getStatus() != 200) {
            System.err.println("=== GetMe Error Debug ===");
            System.err.println("Status: " + result.getResponse().getStatus());
            System.err.println("Response: " + result.getResponse().getContentAsString());
            System.err.println("UserId: " + userId);
            System.err.println("CompanyId: " + companyId);
            System.err.println("=========================");
        }

        mockMvc.perform(get("/v1/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @DisplayName("異常系：無効なトークン")
    void testGetMe_InvalidToken() throws Exception {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        mockMvc.perform(get("/v1/auth/me")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("異常系：トークンなし")
    void testGetMe_NoToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
