package com.auratime.api.v1.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * エラーレスポンスの統一フォーマット
 *
 * <p>
 * すべてのAPIエラーレスポンスを統一された形式で返すためのクラスです。
 * エラー発生時に使用されます。
 * </p>
 *
 * <h3>エラーレスポンス形式:</h3>
 * 
 * <pre>{@code
 * {
 *   "success": false,
 *   "error": {
 *     "code": "ERROR_CODE",
 *     "message": "エラーメッセージ",
 *     "details": ["詳細1", "詳細2"]
 *   },
 *   "requestId": "uuid-string"
 * }
 * }</pre>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    /** 成功フラグ（常にfalse） */
    @Builder.Default
    private boolean success = false;

    /** エラー詳細情報 */
    private ErrorDetail error;

    /** リクエストID（リクエスト追跡用） */
    private String requestId;

    /**
     * エラー詳細情報
     *
     * <p>
     * エラーのコード、メッセージ、詳細情報を保持します。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        /** エラーコード（例: "BAD_CREDENTIALS", "VALIDATION_ERROR"） */
        private String code;

        /** エラーメッセージ */
        private String message;

        /** エラー詳細リスト（バリデーションエラーなどで使用） */
        private List<String> details;
    }
}
