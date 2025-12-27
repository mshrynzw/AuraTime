package com.auratime.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auratime.domain.AuditLog;

/**
 * 監査ログリポジトリ
 *
 * <p>
 * 監査ログエンティティに対するデータアクセス操作を提供するリポジトリインターフェースです。
 * 監査ログの検索とフィルタリング機能を提供します。
 * </p>
 *
 * <h3>ソフト削除対応</h3>
 * <p>
 * すべてのクエリメソッドは、deletedAtがnullのレコード（有効なレコード）のみを対象とします。
 * これにより、削除済み監査ログが検索結果に含まれることを防ぎます。
 * </p>
 *
 * <h3>マルチテナント対応</h3>
 * <p>
 * 監査ログはcompany_idによって分離され、各会社は自社の監査ログのみを参照できます。
 * すべてのクエリメソッドは、company_idによるフィルタリングを必須とします。
 * </p>
 *
 * <h3>ページング対応</h3>
 * <p>
 * 監査ログは大量になる可能性があるため、ページング機能を提供します。
 * 結果は発生日時（happened_at）の降順でソートされます。
 * </p>
 *
 * @see com.auratime.domain.AuditLog
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * 会社IDとフィルター条件で監査ログを検索（削除済みを除く）
     *
     * <p>
     * 指定された会社の監査ログを、複数のフィルター条件で検索します。
     * すべてのフィルター条件はオプション（null可）で、nullの場合はその条件を無視します。
     * </p>
     *
     * <h3>フィルター条件</h3>
     * <ul>
     * <li><strong>companyId</strong>: 会社ID（必須）</li>
     * <li><strong>targetType</strong>: 対象テーブル名（例: "m_users"、null可）</li>
     * <li><strong>action</strong>: 操作種別（"create"|"update"|"delete"、null可）</li>
     * <li><strong>actorUserId</strong>: 実行者ユーザーID（null可）</li>
     * <li><strong>startDate</strong>: 開始日時（null可）</li>
     * <li><strong>endDate</strong>: 終了日時（null可）</li>
     * </ul>
     *
     * <h3>ソート</h3>
     * <p>
     * 結果は発生日時（happened_at）の降順でソートされます。
     * 最新の監査ログが最初に表示されます。
     * </p>
     *
     * <h3>使用例</h3>
     * <ul>
     * <li>特定のテーブル（例: "m_users"）の変更履歴を取得</li>
     * <li>特定のユーザーが実行した操作の履歴を取得</li>
     * <li>特定の期間の監査ログを取得</li>
     * <li>監査ログ一覧画面での検索・フィルタリング</li>
     * </ul>
     *
     * @param companyId   会社ID（必須）
     * @param targetType  対象テーブル名（null可）
     * @param action      操作種別（null可）
     * @param actorUserId 実行者ユーザーID（null可）
     * @param startDate   開始日時（null可）
     * @param endDate     終了日時（null可）
     * @param pageable    ページング情報
     * @return 検索結果のページ
     */
    @Query("SELECT al FROM AuditLog al " +
            "WHERE al.companyId = :companyId " +
            "AND (:targetType IS NULL OR al.targetType = :targetType) " +
            "AND (:action IS NULL OR al.action = :action) " +
            "AND (:actorUserId IS NULL OR al.actorUserId = :actorUserId) " +
            "AND (:startDate IS NULL OR al.happenedAt >= :startDate) " +
            "AND (:endDate IS NULL OR al.happenedAt <= :endDate) " +
            "AND al.deletedAt IS NULL " +
            "ORDER BY al.happenedAt DESC")
    Page<AuditLog> findByCompanyIdAndFilters(
            @Param("companyId") UUID companyId,
            @Param("targetType") String targetType,
            @Param("action") String action,
            @Param("actorUserId") UUID actorUserId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);
}
