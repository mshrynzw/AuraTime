package com.auratime.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.auratime.domain.Company;
import com.auratime.domain.CompanyMembership;
import com.auratime.domain.User;
import com.auratime.repository.CompanyMembershipRepository;
import com.auratime.repository.CompanyRepository;
import com.auratime.repository.UserRepository;
import com.auratime.util.JwtTokenProvider;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 認可機能の統合テスト
 *
 * <p>
 * ロールベースアクセス制御（RBAC）が正しく機能することを確認するテストクラスです。
 * 異なるロール（employee、admin）を持つユーザーを作成し、
 * 認証・認可の動作を検証します。
 * </p>
 *
 * <h3>テスト対象</h3>
 * <ul>
 * <li>適切なロールでのアクセス許可</li>
 * <li>認証なしでのアクセス拒否</li>
 * <li>異なるロール（employee、admin）でのアクセス</li>
 * </ul>
 *
 * <h3>テスト方針</h3>
 * <ul>
 * <li>EmployeeロールとAdminロールのユーザーを作成</li>
 * <li>各ロールでJWTトークンを生成</li>
 * <li>認証済みユーザーはアクセス可能であることを確認</li>
 * <li>認証なしユーザーはアクセス不可であることを確認</li>
 * </ul>
 *
 * <h3>注意事項</h3>
 * <p>
 * 現在の実装では、/v1/auth/meエンドポイントは認証済みユーザーであれば
 * すべてのロールでアクセス可能です。
 * 将来的に、ロールごとのアクセス制御が必要なエンドポイントが追加された場合、
 * @PreAuthorizeアノテーションを使用してテストを拡張してください。
 * </p>
 *
 * @see com.auratime.config.SecurityConfig
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("認可テスト")
class AuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyMembershipRepository companyMembershipRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PersistenceContext
    private EntityManager entityManager;

    private UUID systemBotUserId;
    private UUID companyId;
    private User employeeUser;
    private User adminUser;
    private Company testCompany;

    @BeforeEach
    @SuppressWarnings("null")
    void setUp() {
        // システムボットユーザーを作成
        systemBotUserId = UUID.randomUUID();
        User systemBot = User.builder()
                .id(systemBotUserId)
                .email("system-bot@auratime.com")
                .passwordHash("dummy")
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

        // Employeeユーザーを作成
        UUID employeeUserId = UUID.randomUUID();
        employeeUser = User.builder()
                .id(employeeUserId)
                .email("employee@example.com")
                .passwordHash("dummy")
                .familyName("従業員")
                .firstName("太郎")
                .status("active")
                .createdBy(systemBotUserId)
                .updatedBy(systemBotUserId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        userRepository.save(employeeUser);

        CompanyMembership employeeMembership = CompanyMembership.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(employeeUserId)
                .role("employee")
                .createdBy(systemBotUserId)
                .updatedBy(systemBotUserId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        companyMembershipRepository.save(employeeMembership);

        // Adminユーザーを作成
        UUID adminUserId = UUID.randomUUID();
        adminUser = User.builder()
                .id(adminUserId)
                .email("admin@example.com")
                .passwordHash("dummy")
                .familyName("管理者")
                .firstName("花子")
                .status("active")
                .createdBy(systemBotUserId)
                .updatedBy(systemBotUserId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        userRepository.save(adminUser);

        CompanyMembership adminMembership = CompanyMembership.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .userId(adminUserId)
                .role("admin")
                .createdBy(systemBotUserId)
                .updatedBy(systemBotUserId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        companyMembershipRepository.save(adminMembership);

        // データベースに確実に保存するため、flush()を呼び出す
        // @Transactionalの影響で、setUpで作成したデータがloginメソッドで見つからない場合があるため
        entityManager.flush();
    }

    @Test
    @DisplayName("正常系：適切なロールでアクセス可能")
    @SuppressWarnings("null")
    void testAccessWithValidRole() throws Exception {
        // Given: Employeeロールでトークンを生成
        UUID userId = employeeUser.getId();

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

        // When & Then: /meエンドポイントにアクセス（認証済みユーザーはアクセス可能）
        mockMvc.perform(get("/v1/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("異常系：認証なしでアクセス不可")
    void testAccessWithoutAuthentication() throws Exception {
        // When & Then: 認証なしでアクセス
        mockMvc.perform(get("/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("正常系：Adminロールでアクセス可能")
    @SuppressWarnings("null")
    void testAccessWithAdminRole() throws Exception {
        // Given: Adminロールでトークンを生成
        UUID userId = adminUser.getId();

        // 会社メンバーシップが存在することを確認（setUpで作成済み）
        var existingMembership = companyMembershipRepository
                .findByUserIdAndCompanyIdAndDeletedAtIsNull(userId, companyId);

        if (existingMembership.isEmpty()) {
            // 会社メンバーシップが存在しない場合は作成
            CompanyMembership membership = CompanyMembership.builder()
                    .id(UUID.randomUUID())
                    .companyId(companyId)
                    .userId(userId)
                    .role("admin")
                    .createdBy(systemBotUserId)
                    .updatedBy(systemBotUserId)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            companyMembershipRepository.save(membership);
        }

        entityManager.flush();
        entityManager.clear(); // キャッシュをクリアして、データベースから再読み込み

        String token = jwtTokenProvider.generateToken(userId, companyId, "admin");

        // When & Then: /meエンドポイントにアクセス
        mockMvc.perform(get("/v1/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
