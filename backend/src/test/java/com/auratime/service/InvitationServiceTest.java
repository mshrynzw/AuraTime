package com.auratime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import org.springframework.web.server.ResponseStatusException;

import com.auratime.api.v1.dto.CreateInvitationRequest;
import com.auratime.api.v1.dto.InvitationResponse;
import com.auratime.domain.Company;
import com.auratime.domain.UserInvitation;
import com.auratime.repository.CompanyMembershipRepository;
import com.auratime.repository.CompanyRepository;
import com.auratime.repository.UserInvitationRepository;
import com.auratime.util.CompanyContext;

/**
 * InvitationServiceの単体テスト
 *
 * <p>
 * InvitationServiceのビジネスロジックをテストするクラスです。
 * Mockitoを使用して依存関係をモック化し、各メソッドの正常系・異常系をテストします。
 * </p>
 *
 * <h3>テスト対象</h3>
 * <ul>
 * <li>招待トークン作成（createInvitation）</li>
 * <li>招待情報取得（getInvitationInfo）</li>
 * </ul>
 *
 * @see com.auratime.service.InvitationService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationService テスト")
class InvitationServiceTest {

    @Mock
    private UserInvitationRepository invitationRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMembershipRepository companyMembershipRepository;

    @InjectMocks
    private InvitationService invitationService;

    private UUID companyId;
    private UUID userId;
    private Company testCompany;
    private UserInvitation testInvitation;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testCompany = Company.builder()
                .id(companyId)
                .name("テスト会社")
                .code("TEST001")
                .maxUsers(10)
                .build();

        testInvitation = UserInvitation.builder()
                .id(UUID.randomUUID())
                .email("invited@example.com")
                .token("test-token-123")
                .role("employee")
                .employeeNo("EMP001")
                .employmentType("fulltime")
                .hireDate(LocalDate.now())
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .maxUses(1)
                .usedCount(0)
                .status("pending")
                .createdBy(userId)
                .updatedBy(userId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // CompanyContextを設定
        CompanyContext.setCompanyId(companyId);
    }

    @Test
    @DisplayName("正常系：招待トークン作成")
    @SuppressWarnings("null")
    void testCreateInvitation_Success() {
        // Given
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setEmail("newuser@example.com");
        request.setRole("employee");
        request.setEmployeeNo("EMP002");
        request.setEmploymentType("fulltime");
        request.setHireDate(LocalDate.now());
        request.setExpiresInDays(7);

        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.of(testCompany));
        when(companyMembershipRepository.countByCompanyIdAndDeletedAtIsNull(companyId)).thenReturn(5L);
        when(invitationRepository.save(any(UserInvitation.class))).thenAnswer(invocation -> {
            UserInvitation invitation = invocation.getArgument(0);
            if (invitation.getId() == null) {
                invitation.setId(UUID.randomUUID());
            }
            invitation.setCompany(testCompany);
            return invitation;
        });

        // When
        UserInvitation result = invitationService.createInvitation(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("newuser@example.com");
        assertThat(result.getRole()).isEqualTo("employee");
        assertThat(result.getEmployeeNo()).isEqualTo("EMP002");
        verify(companyRepository).findByIdAndDeletedAtIsNull(companyId);
        verify(companyMembershipRepository).countByCompanyIdAndDeletedAtIsNull(companyId);
        verify(invitationRepository).save(any(UserInvitation.class));
    }

    @Test
    @DisplayName("異常系：ライセンス数上限超過")
    void testCreateInvitation_LicenseExceeded() {
        // Given
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setEmail("newuser@example.com");
        request.setRole("employee");
        request.setEmployeeNo("EMP002");

        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.of(testCompany));
        when(companyMembershipRepository.countByCompanyIdAndDeletedAtIsNull(companyId)).thenReturn(10L);

        // When & Then
        assertThatThrownBy(() -> invitationService.createInvitation(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode().value()).isEqualTo(403);
                    assertThat(ex.getReason()).contains("ライセンス数の上限に達しています");
                });

        verify(companyRepository).findByIdAndDeletedAtIsNull(companyId);
        verify(companyMembershipRepository).countByCompanyIdAndDeletedAtIsNull(companyId);
        verify(invitationRepository, never()).save(any(UserInvitation.class));
    }

    @Test
    @DisplayName("異常系：会社が見つからない")
    void testCreateInvitation_CompanyNotFound() {
        // Given
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setEmail("newuser@example.com");
        request.setRole("employee");
        request.setEmployeeNo("EMP002");

        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> invitationService.createInvitation(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode().value()).isEqualTo(404);
                });

        verify(companyRepository).findByIdAndDeletedAtIsNull(companyId);
        verify(invitationRepository, never()).save(any(UserInvitation.class));
    }

    @Test
    @DisplayName("正常系：招待情報取得")
    void testGetInvitationInfo_Success() {
        // Given
        String token = "test-token-123";
        testInvitation.setCompany(testCompany);

        when(invitationRepository.findByTokenAndDeletedAtIsNullAndStatusPending(token))
                .thenReturn(Optional.of(testInvitation));

        // When
        InvitationResponse response = invitationService.getInvitationByToken(token);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("invited@example.com");
        assertThat(response.getRole()).isEqualTo("employee");
        assertThat(response.getEmployeeNo()).isEqualTo("EMP001");
        assertThat(response.getCompanyId()).isEqualTo(companyId);
        assertThat(response.getCompanyName()).isEqualTo("テスト会社");
        verify(invitationRepository).findByTokenAndDeletedAtIsNullAndStatusPending(token);
    }

    @Test
    @DisplayName("異常系：招待トークンが見つからない")
    void testGetInvitationInfo_TokenNotFound() {
        // Given
        String token = "invalid-token";

        when(invitationRepository.findByTokenAndDeletedAtIsNullAndStatusPending(token))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> invitationService.getInvitationByToken(token))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode().value()).isEqualTo(404);
                    assertThat(ex.getReason()).contains("招待トークンが見つかりません");
                });

        verify(invitationRepository).findByTokenAndDeletedAtIsNullAndStatusPending(token);
    }

    @Test
    @DisplayName("異常系：招待トークンが期限切れ")
    void testGetInvitationInfo_TokenExpired() {
        // Given
        String token = "expired-token";
        testInvitation.setExpiresAt(OffsetDateTime.now().minusDays(1));
        testInvitation.setStatus("expired");

        when(invitationRepository.findByTokenAndDeletedAtIsNullAndStatusPending(token))
                .thenReturn(Optional.of(testInvitation));

        // When & Then
        assertThatThrownBy(() -> invitationService.getInvitationByToken(token))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException ex = (ResponseStatusException) exception;
                    assertThat(ex.getStatusCode().value()).isEqualTo(400);
                    assertThat(ex.getReason()).contains("招待トークンが無効または期限切れです");
                });

        verify(invitationRepository).findByTokenAndDeletedAtIsNullAndStatusPending(token);
    }

    @Test
    @DisplayName("正常系：ライセンス数チェックなし（maxUsersがnull）")
    @SuppressWarnings("null")
    void testCreateInvitation_NoLicenseCheck() {
        // Given
        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setEmail("newuser@example.com");
        request.setRole("employee");
        request.setEmployeeNo("EMP002");

        Company companyWithoutLimit = Company.builder()
                .id(companyId)
                .name("テスト会社")
                .code("TEST001")
                .maxUsers(null) // ライセンス数制限なし
                .build();

        when(companyRepository.findByIdAndDeletedAtIsNull(companyId)).thenReturn(Optional.of(companyWithoutLimit));
        when(invitationRepository.save(any(UserInvitation.class))).thenAnswer(invocation -> {
            UserInvitation invitation = invocation.getArgument(0);
            if (invitation.getId() == null) {
                invitation.setId(UUID.randomUUID());
            }
            invitation.setCompany(companyWithoutLimit);
            return invitation;
        });

        // When
        UserInvitation result = invitationService.createInvitation(request);

        // Then
        assertThat(result).isNotNull();
        verify(companyRepository).findByIdAndDeletedAtIsNull(companyId);
        verify(companyMembershipRepository, never()).countByCompanyIdAndDeletedAtIsNull(any());
        verify(invitationRepository).save(any(UserInvitation.class));
    }
}

