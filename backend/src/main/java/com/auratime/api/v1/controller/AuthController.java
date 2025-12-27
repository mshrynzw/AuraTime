package com.auratime.api.v1.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auratime.api.v1.dto.ApiResponse;
import com.auratime.api.v1.dto.LoginRequest;
import com.auratime.api.v1.dto.LoginResponse;
import com.auratime.api.v1.dto.MeResponse;
import com.auratime.api.v1.dto.RegisterRequest;
import com.auratime.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 認証関連のRESTコントローラー
 *
 * <p>
 * ユーザー登録、ログイン、現在のユーザー情報取得などの認証関連のエンドポイントを提供します。
 * </p>
 *
 * <h3>エンドポイント:</h3>
 * <ul>
 * <li>{@code POST /api/v1/auth/register} - ユーザー登録</li>
 * <li>{@code POST /api/v1/auth/login} - ログイン（JWTトークン発行）</li>
 * <li>{@code GET /api/v1/auth/me} - 現在のユーザー情報取得（認証必須）</li>
 * </ul>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * ユーザー登録
     *
     * <p>
     * 新規ユーザーを登録します。メールアドレスとパスワード、氏名を必要とします。
     * </p>
     *
     * @param request     登録リクエスト（メールアドレス、パスワード、氏名など）
     * @param httpRequest HTTPリクエスト（リクエストID取得用）
     * @return 登録成功レスポンス（HTTP 201 Created）
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @RequestBody @Valid RegisterRequest request,
            HttpServletRequest httpRequest) {
        log.info("User registration request: email={}", request.getEmail());
        authService.register(request);
        String requestId = getRequestId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, requestId));
    }

    /**
     * ログイン
     *
     * <p>
     * メールアドレスとパスワードで認証し、JWTトークンを発行します。
     * </p>
     *
     * @param request     ログインリクエスト（メールアドレス、パスワード）
     * @param httpRequest HTTPリクエスト（リクエストID取得用）
     * @return ログイン成功レスポンス（JWTトークンとユーザー情報を含む）
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("Login request: email={}", request.getEmail());
        LoginResponse response = authService.login(request);
        String requestId = getRequestId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, requestId));
    }

    /**
     * 現在のユーザー情報取得
     *
     * <p>
     * 認証済みユーザーの情報を取得します。JWTトークンからユーザーIDを取得し、
     * ユーザー情報を返します。
     * </p>
     *
     * @param authentication Spring Securityの認証情報（JWTトークンから取得）
     * @param httpRequest    HTTPリクエスト（リクエストID取得用）
     * @return 現在のユーザー情報レスポンス
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> getCurrentUser(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        MeResponse response = authService.getCurrentUser(authentication);
        String requestId = getRequestId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, requestId));
    }

    /**
     * リクエストIDを取得
     *
     * <p>
     * HTTPヘッダーからリクエストIDを取得します。
     * ヘッダーに存在しない場合は、新規にUUIDを生成します。
     * </p>
     *
     * @param request HTTPリクエスト
     * @return リクエストID（既存のもの、または新規生成されたUUID）
     */
    private String getRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }
}
