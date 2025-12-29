package com.auratime.api.v1.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auratime.api.v1.dto.ApiResponse;
import com.auratime.api.v1.dto.CreateInvitationRequest;
import com.auratime.api.v1.dto.InvitationResponse;
import com.auratime.domain.UserInvitation;
import com.auratime.service.InvitationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 招待関連のRESTコントローラー
 *
 * <p>
 * ユーザー招待に関するエンドポイントを提供します。
 * </p>
 *
 * <h3>エンドポイント:</h3>
 * <ul>
 * <li>{@code POST /api/v1/invitations} - 招待トークン発行（Admin権限）</li>
 * </ul>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/invitations")
@RequiredArgsConstructor
@Slf4j
public class InvitationController {

    private final InvitationService invitationService;

    /**
     * 招待トークン発行
     *
     * <p>
     * 管理者が新しいユーザーを招待するためにトークンを発行します。
     * </p>
     *
     * @param request     招待作成リクエスト
     * @param httpRequest HTTPリクエスト（リクエストID取得用）
     * @return 招待作成成功レスポンス（HTTP 201 Created）
     */
    @PostMapping
    public ResponseEntity<ApiResponse<InvitationResponse>> createInvitation(
            @RequestBody @Valid CreateInvitationRequest request,
            HttpServletRequest httpRequest) {
        log.info("Creating invitation: email={}, role={}", request.getEmail(), request.getRole());
        UserInvitation invitation = invitationService.createInvitation(request);
        String requestId = getRequestId(httpRequest);

        // レスポンスを作成（トークンは含めない）
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

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, requestId));
    }

    /**
     * リクエストIDを取得
     *
     * @param request HTTPリクエスト
     * @return リクエストID
     */
    private String getRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }
}

