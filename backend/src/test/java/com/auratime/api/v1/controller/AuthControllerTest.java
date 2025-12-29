package com.auratime.api.v1.controller;

import com.auratime.api.v1.dto.*;
import com.auratime.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthControllerの単体テスト
 *
 * <p>
 * AuthControllerのHTTPリクエスト処理をテストするクラスです。
 * @WebMvcTestを使用して、コントローラーレイヤーのみをテストします。
 * </p>
 *
 * <h3>テスト対象</h3>
 * <ul>
 *   <li>ユーザー登録エンドポイント（POST /v1/auth/register）</li>
 *   <li>ログインエンドポイント（POST /v1/auth/login）</li>
 * </ul>
 *
 * <h3>テスト方針</h3>
 * <ul>
 *   <li>正常系: 適切なリクエストで正しいレスポンスが返ることを確認</li>
 *   <li>異常系: バリデーションエラーで適切なHTTPステータスコードが返ることを確認</li>
 *   <li>レスポンス形式: ApiResponse形式で返ることを確認</li>
 * </ul>
 *
 * <h3>注意事項</h3>
 * <p>
 * このテストはコントローラーレイヤーのみをテストするため、
 * セキュリティフィルターやサービスの実装はモック化されています。
 * 実際のHTTPリクエスト処理をテストするには、統合テスト（AuthIntegrationTest）を参照してください。
 * </p>
 *
 * @see com.auratime.api.v1.controller.AuthController
 * @see com.auratime.integration.AuthIntegrationTest
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController テスト")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private com.auratime.util.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.auratime.service.PasswordResetService passwordResetService;

    @MockBean
    private com.auratime.service.InvitationService invitationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("正常系：ユーザー登録")
    void testRegister_Success() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setFamilyName("佐藤");
        request.setFirstName("花子");

        // When & Then
        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("異常系：バリデーションエラー（メールアドレス未入力）")
    void testRegister_ValidationError_EmailRequired() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setPassword("password123");
        request.setFamilyName("佐藤");
        request.setFirstName("花子");

        // When & Then
        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("異常系：バリデーションエラー（無効なメールアドレス形式）")
    void testRegister_ValidationError_InvalidEmail() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");
        request.setPassword("password123");
        request.setFamilyName("佐藤");
        request.setFirstName("花子");

        // When & Then
        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("正常系：ログイン")
    void testLogin_Success() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        LoginResponse response = LoginResponse.builder()
                .token("test-jwt-token")
                .user(LoginResponse.UserInfo.builder()
                        .id(userId)
                        .email("test@example.com")
                        .familyName("山田")
                        .firstName("太郎")
                        .companyId(companyId)
                        .role("employee")
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("異常系：ログイン - バリデーションエラー")
    void testLogin_ValidationError() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        // メールアドレスとパスワードを未設定

        // When & Then
        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("正常系：招待情報取得")
    void testGetInvitationInfo_Success() throws Exception {
        // Given
        String token = "valid-token-123";
        UUID companyId = UUID.randomUUID();
        InvitationResponse response = new InvitationResponse();
        response.setId(UUID.randomUUID());
        response.setCompanyId(companyId);
        response.setCompanyName("テスト会社");
        response.setEmail("invited@example.com");
        response.setRole("employee");
        response.setEmployeeNo("EMP001");

        when(invitationService.getInvitationByToken(token)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/v1/auth/invitations/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("invited@example.com"))
                .andExpect(jsonPath("$.data.role").value("employee"));
    }

    @Test
    @DisplayName("正常系：パスワードリセット要求")
    void testRequestPasswordReset_Success() throws Exception {
        // Given
        PasswordResetRequestRequest request = new PasswordResetRequestRequest();
        request.setEmail("test@example.com");

        // When & Then
        mockMvc.perform(post("/v1/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("異常系：パスワードリセット要求 - バリデーションエラー")
    void testRequestPasswordReset_ValidationError() throws Exception {
        // Given
        PasswordResetRequestRequest request = new PasswordResetRequestRequest();
        // メールアドレスを未設定

        // When & Then
        mockMvc.perform(post("/v1/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("正常系：パスワードリセット実行")
    void testConfirmPasswordReset_Success() throws Exception {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken("reset-token-123");
        request.setNewPassword("NewPassword123!");

        // When & Then
        mockMvc.perform(post("/v1/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("異常系：パスワードリセット実行 - バリデーションエラー（パスワード未入力）")
    void testConfirmPasswordReset_ValidationError_PasswordRequired() throws Exception {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken("reset-token-123");
        // パスワードを未設定

        // When & Then
        mockMvc.perform(post("/v1/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("異常系：パスワードリセット実行 - バリデーションエラー（パスワード要件不満足）")
    void testConfirmPasswordReset_ValidationError_PasswordPattern() throws Exception {
        // Given
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest();
        request.setToken("reset-token-123");
        request.setNewPassword("short"); // 12文字未満、要件を満たさない

        // When & Then
        mockMvc.perform(post("/v1/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

