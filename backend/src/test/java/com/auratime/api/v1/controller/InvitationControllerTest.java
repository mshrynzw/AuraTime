package com.auratime.api.v1.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.auratime.api.v1.dto.CreateInvitationRequest;
import com.auratime.api.v1.dto.InvitationResponse;
import com.auratime.domain.Company;
import com.auratime.domain.UserInvitation;
import com.auratime.service.InvitationService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * InvitationControllerの単体テスト
 *
 * <p>
 * InvitationControllerのHTTPリクエスト処理をテストするクラスです。
 * @WebMvcTestを使用して、コントローラーレイヤーのみをテストします。
 * </p>
 *
 * <h3>テスト対象</h3>
 * <ul>
 *   <li>招待トークン作成エンドポイント（POST /v1/invitations）</li>
 * </ul>
 *
 * @see com.auratime.api.v1.controller.InvitationController
 */
@WebMvcTest(controllers = InvitationController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("InvitationController テスト")
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvitationService invitationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("正常系：招待トークン作成")
    void testCreateInvitation_Success() throws Exception {
        // Given
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setEmail("newuser@example.com");
        request.setRole("employee");
        request.setEmployeeNo("EMP001");
        request.setEmploymentType("fulltime");
        request.setHireDate(LocalDate.now());
        request.setExpiresInDays(7);

        UUID companyId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        Company company = Company.builder()
                .id(companyId)
                .name("テスト会社")
                .code("TEST001")
                .build();

        UserInvitation invitation = UserInvitation.builder()
                .id(invitationId)
                .company(company)
                .email("newuser@example.com")
                .role("employee")
                .employeeNo("EMP001")
                .employmentType("fulltime")
                .hireDate(LocalDate.now())
                .build();

        when(invitationService.createInvitation(any(CreateInvitationRequest.class)))
                .thenReturn(invitation);

        // When & Then
        mockMvc.perform(post("/v1/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.role").value("employee"))
                .andExpect(jsonPath("$.data.employeeNo").value("EMP001"));
    }

    @Test
    @DisplayName("異常系：バリデーションエラー（メールアドレス未入力）")
    void testCreateInvitation_ValidationError_EmailRequired() throws Exception {
        // Given
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setRole("employee");
        request.setEmployeeNo("EMP001");
        // メールアドレスを未設定

        // When & Then
        mockMvc.perform(post("/v1/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("異常系：バリデーションエラー（無効なメールアドレス形式）")
    void testCreateInvitation_ValidationError_InvalidEmail() throws Exception {
        // Given
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setEmail("invalid-email");
        request.setRole("employee");
        request.setEmployeeNo("EMP001");

        // When & Then
        mockMvc.perform(post("/v1/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("異常系：バリデーションエラー（社員番号未入力）")
    void testCreateInvitation_ValidationError_EmployeeNoRequired() throws Exception {
        // Given
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setEmail("newuser@example.com");
        request.setRole("employee");
        // 社員番号を未設定

        // When & Then
        mockMvc.perform(post("/v1/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

