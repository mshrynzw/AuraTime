package com.auratime.api.v1.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.auratime.api.v1.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

/**
 * GlobalExceptionHandlerの単体テスト
 *
 * <p>
 * グローバル例外ハンドラーの動作をテストするクラスです。
 * 各例外タイプに対して、適切なHTTPステータスコードとエラーレスポンスが返されることを確認します。
 * </p>
 *
 * <h3>テスト対象</h3>
 * <ul>
 * <li>BadCredentialsExceptionのハンドリング（401 Unauthorized）</li>
 * <li>IllegalArgumentExceptionのハンドリング（400 Bad Request）</li>
 * <li>IllegalStateExceptionのハンドリング（400 Bad Request）</li>
 * <li>MethodArgumentNotValidExceptionのハンドリング（400 Bad Request、バリデーション詳細）</li>
 * <li>その他のExceptionのハンドリング（500 Internal Server Error）</li>
 * <li>リクエストIDの取得（ヘッダーあり/なし）</li>
 * </ul>
 *
 * <h3>テスト方針</h3>
 * <ul>
 * <li>各例外ハンドラーが正しいHTTPステータスコードを返すことを確認</li>
 * <li>エラーレスポンスの形式が正しいことを確認</li>
 * <li>エラーコードとメッセージが正しく設定されることを確認</li>
 * <li>リクエストIDが正しく設定されることを確認</li>
 * </ul>
 *
 * @see com.auratime.api.v1.exception.GlobalExceptionHandler
 */
@DisplayName("GlobalExceptionHandler テスト")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        mockRequest = mock(HttpServletRequest.class);
    }

    @Test
    @DisplayName("正常系：BadCredentialsExceptionのハンドリング")
    void testHandleBadCredentials() {
        // Given
        String errorMessage = "メールアドレスまたはパスワードが正しくありません";
        BadCredentialsException exception = new BadCredentialsException(errorMessage);
        String requestId = UUID.randomUUID().toString();
        when(mockRequest.getHeader("X-Request-Id")).thenReturn(requestId);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentials(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ErrorResponse body = Objects.requireNonNull(response.getBody(), "Response body should not be null");
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getError()).isNotNull();
        assertThat(body.getError().getCode()).isEqualTo("BAD_CREDENTIALS");
        assertThat(body.getError().getMessage()).isEqualTo(errorMessage);
        assertThat(body.getRequestId()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("正常系：IllegalArgumentExceptionのハンドリング")
    void testHandleIllegalArgument() {
        // Given
        String errorMessage = "このメールアドレスは既に登録されています";
        IllegalArgumentException exception = new IllegalArgumentException(errorMessage);
        String requestId = UUID.randomUUID().toString();
        when(mockRequest.getHeader("X-Request-Id")).thenReturn(requestId);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = Objects.requireNonNull(response.getBody(), "Response body should not be null");
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getError()).isNotNull();
        assertThat(body.getError().getCode()).isEqualTo("INVALID_PARAMETER");
        assertThat(body.getError().getMessage()).isEqualTo(errorMessage);
        assertThat(body.getRequestId()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("正常系：IllegalStateExceptionのハンドリング")
    void testHandleIllegalState() {
        // Given
        String errorMessage = "このアカウントは無効化されています";
        IllegalStateException exception = new IllegalStateException(errorMessage);
        String requestId = UUID.randomUUID().toString();
        when(mockRequest.getHeader("X-Request-Id")).thenReturn(requestId);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalState(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = Objects.requireNonNull(response.getBody(), "Response body should not be null");
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getError()).isNotNull();
        assertThat(body.getError().getCode()).isEqualTo("ILLEGAL_STATE");
        assertThat(body.getError().getMessage()).isEqualTo(errorMessage);
        assertThat(body.getRequestId()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("正常系：MethodArgumentNotValidExceptionのハンドリング（バリデーションエラー）")
    void testHandleValidation() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("registerRequest", "email", "メールアドレスは必須です");
        FieldError fieldError2 = new FieldError("registerRequest", "password", "パスワードは必須です");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.Arrays.asList(fieldError1, fieldError2));

        String requestId = UUID.randomUUID().toString();
        when(mockRequest.getHeader("X-Request-Id")).thenReturn(requestId);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidation(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = Objects.requireNonNull(response.getBody(), "Response body should not be null");
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getError()).isNotNull();
        assertThat(body.getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(body.getError().getMessage()).isEqualTo("入力値が不正です");
        assertThat(body.getError().getDetails()).isNotNull();
        assertThat(body.getError().getDetails()).hasSize(2);
        assertThat(body.getError().getDetails()).contains("email: メールアドレスは必須です");
        assertThat(body.getError().getDetails()).contains("password: パスワードは必須です");
        assertThat(body.getRequestId()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("正常系：その他のExceptionのハンドリング")
    void testHandleGenericException() {
        // Given
        String errorMessage = "予期しないエラー";
        RuntimeException exception = new RuntimeException(errorMessage);
        String requestId = UUID.randomUUID().toString();
        when(mockRequest.getHeader("X-Request-Id")).thenReturn(requestId);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = Objects.requireNonNull(response.getBody(), "Response body should not be null");
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getError()).isNotNull();
        assertThat(body.getError().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(body.getError().getMessage()).isEqualTo("予期しないエラーが発生しました");
        assertThat(body.getRequestId()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("正常系：リクエストIDがヘッダーに存在する場合")
    void testRequestId_FromHeader() {
        // Given
        String requestId = UUID.randomUUID().toString();
        when(mockRequest.getHeader("X-Request-Id")).thenReturn(requestId);
        IllegalArgumentException exception = new IllegalArgumentException("テストエラー");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(exception, mockRequest);

        // Then
        ErrorResponse body = Objects.requireNonNull(response.getBody(), "Response body should not be null");
        assertThat(body.getRequestId()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("正常系：リクエストIDがヘッダーに存在しない場合（新規生成）")
    void testRequestId_Generated() {
        // Given
        when(mockRequest.getHeader("X-Request-Id")).thenReturn(null);
        IllegalArgumentException exception = new IllegalArgumentException("テストエラー");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(exception, mockRequest);

        // Then
        ErrorResponse body = Objects.requireNonNull(response.getBody(), "Response body should not be null");
        assertThat(body.getRequestId()).isNotNull();
        assertThat(body.getRequestId()).isNotEmpty();
        // UUID形式であることを確認
        UUID.fromString(body.getRequestId());
    }

    @Test
    @DisplayName("正常系：リクエストIDが空文字列の場合（新規生成）")
    void testRequestId_EmptyString() {
        // Given
        when(mockRequest.getHeader("X-Request-Id")).thenReturn("");
        IllegalArgumentException exception = new IllegalArgumentException("テストエラー");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(exception, mockRequest);

        // Then
        ErrorResponse body = Objects.requireNonNull(response.getBody(), "Response body should not be null");
        assertThat(body.getRequestId()).isNotNull();
        assertThat(body.getRequestId()).isNotEmpty();
        // UUID形式であることを確認
        UUID.fromString(body.getRequestId());
    }
}
