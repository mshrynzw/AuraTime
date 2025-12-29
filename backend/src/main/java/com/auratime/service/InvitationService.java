package com.auratime.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.auratime.api.v1.dto.CreateInvitationRequest;
import com.auratime.api.v1.dto.InvitationResponse;
import com.auratime.domain.Company;
import com.auratime.domain.UserInvitation;
import com.auratime.repository.CompanyMembershipRepository;
import com.auratime.repository.CompanyRepository;
import com.auratime.repository.UserInvitationRepository;
import com.auratime.util.CompanyContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 招待サービス
 *
 * <p>
 * ユーザー招待に関するビジネスロジックを実装するサービスクラスです。
 * 招待トークンの発行、検証、情報取得を提供します。
 * </p>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvitationService {

    private final UserInvitationRepository invitationRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMembershipRepository companyMembershipRepository;

    /**
     * 招待トークンを発行
     *
     * <p>
     * 管理者が新しいユーザーを招待するためにトークンを発行します。
     * ライセンス数チェックを行い、超過している場合はエラーを返します。
     * </p>
     *
     * @param request 招待作成リクエスト
     * @return 発行された招待エンティティ
     * @throws ResponseStatusException ライセンス数超過、会社が見つからない場合
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN')")
    public UserInvitation createInvitation(CreateInvitationRequest request) {
        UUID companyId = CompanyContext.getCompanyId();
        log.info("Creating invitation: companyId={}, email={}, role={}", companyId, request.getEmail(), request.getRole());

        // 会社を取得
        Company company = companyRepository.findByIdAndDeletedAtIsNull(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会社が見つかりません"));

        // ライセンス数チェック
        if (company.getMaxUsers() != null) {
            long currentUserCount = companyMembershipRepository.countByCompanyIdAndDeletedAtIsNull(companyId);
            if (currentUserCount >= company.getMaxUsers()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "ライセンス数の上限に達しています。現在のユーザー数: " + currentUserCount + ", 上限: " + company.getMaxUsers());
            }
        }

        // トークンを生成（UUID v4を使用、推測困難）
        String token = UUID.randomUUID().toString();

        // 有効期限を設定（デフォルト7日）
        int expiresInDays = request.getExpiresInDays() != null ? request.getExpiresInDays() : 7;
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(expiresInDays);

        // 招待エンティティを作成
        UserInvitation invitation = UserInvitation.builder()
                .company(company)
                .email(request.getEmail())
                .token(token)
                .role(request.getRole())
                .employeeNo(request.getEmployeeNo())
                .employmentType(request.getEmploymentType())
                .hireDate(request.getHireDate())
                .expiresAt(expiresAt)
                .maxUses(1)
                .usedCount(0)
                .status("pending")
                .build();

        UserInvitation savedInvitation = invitationRepository.save(invitation);

        log.info("Invitation created: invitationId={}, token={}, expiresAt={}",
                savedInvitation.getId(), token, expiresAt);
        return savedInvitation;
    }

    /**
     * 招待情報を取得（公開API）
     *
     * <p>
     * 招待トークンから招待情報を取得します。
     * ユーザー登録画面で使用されます。
     * </p>
     *
     * @param token 招待トークン
     * @return 招待情報レスポンス
     * @throws ResponseStatusException トークンが無効、期限切れ、使用済みの場合
     */
    @Transactional(readOnly = true)
    public InvitationResponse getInvitationByToken(String token) {
        log.info("Getting invitation by token: token={}", token);

        @SuppressWarnings("null")
        UserInvitation invitation = invitationRepository
                .findByTokenAndDeletedAtIsNullAndStatusPending(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "招待トークンが見つかりません"));

        // 有効性チェック
        if (!invitation.isValid()) {
            // 期限切れの場合はステータスを更新
            if (invitation.getExpiresAt().isBefore(OffsetDateTime.now())) {
                invitation.setStatus("expired");
                invitationRepository.save(invitation);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "招待トークンが無効です");
        }

        // レスポンスを作成
        InvitationResponse response = new InvitationResponse();
        response.setId(invitation.getId());
        response.setCompanyId(invitation.getCompany().getId());
        response.setCompanyName(invitation.getCompany().getName());
        response.setEmail(invitation.getEmail());
        response.setRole(invitation.getRole());
        response.setEmployeeNo(invitation.getEmployeeNo());
        response.setEmploymentType(invitation.getEmploymentType());
        response.setHireDate(invitation.getHireDate());
        response.setExpiresAt(invitation.getExpiresAt());

        return response;
    }

    /**
     * 招待トークンを使用済みにマーク
     *
     * <p>
     * ユーザー登録時に招待トークンを使用済みにマークします。
     * </p>
     *
     * @param invitation 招待エンティティ
     * @param user      使用したユーザー
     */
    public void markAsUsed(UserInvitation invitation, com.auratime.domain.User user) {
        invitation.setStatus("used");
        invitation.setUsedAt(OffsetDateTime.now());
        invitation.setUsedBy(user);
        invitation.setUsedCount(invitation.getUsedCount() + 1);
        invitationRepository.save(invitation);
        log.info("Invitation marked as used: invitationId={}, userId={}", invitation.getId(), user.getId());
    }
}

