package com.auratime.api.v1.exception;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.auratime.api.v1.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * グローバル例外ハンドラー
 *
 * <p>
 * アプリケーション全体で発生する例外をキャッチし、統一されたエラーレスポンス形式に変換します。
 * すべてのコントローラーで発生する例外を処理します。
 * </p>
 *
 * <h3>処理する例外:</h3>
 * <ul>
 * <li>{@link BadCredentialsException} - 認証失敗（401 Unauthorized）</li>
 * <li>{@link IllegalArgumentException} - 不正な引数（400 Bad Request）</li>
 * <li>{@link IllegalStateException} - 不正な状態（400 Bad Request）</li>
 * <li>{@link MethodArgumentNotValidException} - バリデーションエラー（400 Bad
 * Request）</li>
 * <li>{@link Exception} - その他の予期しないエラー（500 Internal Server Error）</li>
 * </ul>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 認証失敗例外のハンドリング
     *
     * <p>
     * ログイン時に認証情報が不正な場合に発生します。
     * </p>
     *
     * @param e       認証失敗例外
     * @param request HTTPリクエスト
     * @return エラーレスポンス（HTTP 401 Unauthorized）
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException e, HttpServletRequest request) {
        log.warn("Bad credentials: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .code("BAD_CREDENTIALS")
                        .message(e.getMessage())
                        .build())
                .requestId(getRequestId(request))
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 不正な引数例外のハンドリング
     *
     * <p>
     * メソッドに渡された引数が不正な場合に発生します。
     * </p>
     *
     * @param e       不正な引数例外
     * @param request HTTPリクエスト
     * @return エラーレスポンス（HTTP 400 Bad Request）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Illegal argument: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .code("INVALID_PARAMETER")
                        .message(e.getMessage())
                        .build())
                .requestId(getRequestId(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 不正な状態例外のハンドリング
     *
     * <p>
     * オブジェクトやアプリケーションが不正な状態にある場合に発生します。
     * </p>
     *
     * @param e       不正な状態例外
     * @param request HTTPリクエスト
     * @return エラーレスポンス（HTTP 400 Bad Request）
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException e, HttpServletRequest request) {
        log.warn("Illegal state: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .code("ILLEGAL_STATE")
                        .message(e.getMessage())
                        .build())
                .requestId(getRequestId(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * バリデーション例外のハンドリング
     *
     * <p>
     * リクエストボディのバリデーション（@Valid）に失敗した場合に発生します。
     * フィールドごとのエラー詳細を返します。
     * </p>
     *
     * @param e       バリデーション例外
     * @param request HTTPリクエスト
     * @return エラーレスポンス（HTTP 400 Bad Request、エラー詳細を含む）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .code("VALIDATION_ERROR")
                        .message("入力値が不正です")
                        .details(details)
                        .build())
                .requestId(getRequestId(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * その他の予期しない例外のハンドリング
     *
     * <p>
     * 上記の例外ハンドラーで処理されなかったすべての例外をキャッチします。
     * 予期しないエラーとして扱い、内部エラーを返します。
     * </p>
     *
     * @param e       例外
     * @param request HTTPリクエスト
     * @return エラーレスポンス（HTTP 500 Internal Server Error）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception e, HttpServletRequest request) {
        log.error("Unexpected error", e);
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorDetail.builder()
                        .code("INTERNAL_SERVER_ERROR")
                        .message("予期しないエラーが発生しました")
                        .build())
                .requestId(getRequestId(request))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
