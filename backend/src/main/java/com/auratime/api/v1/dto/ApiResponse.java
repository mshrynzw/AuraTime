package com.auratime.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * APIレスポンスの統一フォーマット
 *
 * <p>
 * すべてのAPIレスポンスを統一された形式で返すためのラッパークラスです。
 * 成功時のレスポンスに使用されます。
 * </p>
 *
 * <h3>レスポンス形式:</h3>
 *
 * <pre>{@code
 * {
 *   "success": true,
 *   "data": { ... },
 *   "requestId": "uuid-string"
 * }
 * }</pre>
 *
 * @param <T> レスポンスデータの型
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    /** 成功フラグ（常にtrue） */
    @Builder.Default
    private boolean success = true;

    /** レスポンスデータ */
    private T data;

    /** リクエストID（リクエスト追跡用） */
    private String requestId;

    /**
     * 成功レスポンスを作成
     *
     * @param <T>       レスポンスデータの型
     * @param data      レスポンスデータ
     * @param requestId リクエストID
     * @return 成功レスポンス
     */
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .requestId(requestId)
                .build();
    }
}
