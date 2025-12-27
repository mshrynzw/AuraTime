package com.auratime.util;

import java.util.UUID;

/**
 * 会社コンテキスト管理クラス
 *
 * <p>
 * マルチテナント分離のため、現在のリクエストに関連付けられた会社IDを
 * ThreadLocalで管理します。各HTTPリクエストは独立したスレッドで処理されるため、
 * ThreadLocalを使用することで、リクエスト間でデータが混在することを防ぎます。
 * </p>
 *
 * <h3>使用例:</h3>
 *
 * <pre>{@code
 * // JWT認証フィルターで設定
 * CompanyContext.setCompanyId(companyId);
 *
 * // サービス層で取得
 * UUID companyId = CompanyContext.getCompanyId();
 *
 * // リクエスト処理後にクリア
 * CompanyContext.clear();
 * }</pre>
 *
 * <h3>注意事項:</h3>
 * <ul>
 * <li>リクエスト処理の開始時にJWTフィルターで設定される</li>
 * <li>リクエスト処理の終了時に必ずクリアする必要がある（メモリリーク防止）</li>
 * <li>非同期処理では別スレッドになるため、明示的に設定が必要</li>
 * </ul>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
public class CompanyContext {
    /** ThreadLocalで管理する会社ID */
    private static final ThreadLocal<UUID> COMPANY_ID = new ThreadLocal<>();

    /**
     * 会社IDを設定
     *
     * <p>
     * 現在のスレッドに関連付けられた会社IDを設定します。
     * 通常はJWT認証フィルターで呼び出されます。
     * </p>
     *
     * @param companyId 会社ID
     */
    public static void setCompanyId(UUID companyId) {
        COMPANY_ID.set(companyId);
    }

    /**
     * 会社IDを取得
     *
     * <p>
     * 現在のスレッドに関連付けられた会社IDを取得します。
     * 設定されていない場合はnullを返します。
     * </p>
     *
     * @return 会社ID（設定されていない場合はnull）
     */
    public static UUID getCompanyId() {
        return COMPANY_ID.get();
    }

    /**
     * 会社IDをクリア
     *
     * <p>
     * 現在のスレッドに関連付けられた会社IDを削除します。
     * リクエスト処理の終了時に必ず呼び出す必要があります（メモリリーク防止）。
     * </p>
     */
    public static void clear() {
        COMPANY_ID.remove();
    }
}
