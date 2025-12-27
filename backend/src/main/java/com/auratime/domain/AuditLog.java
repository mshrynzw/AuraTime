package com.auratime.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 監査ログエンティティ
 *
 * <p>
 * 業務データの変更履歴を記録するエンティティです。
 * Spring AOPを使用して、データの作成・更新・削除時に自動的に記録されます。
 * </p>
 *
 * <h3>記録される情報</h3>
 * <ul>
 * <li>誰が（actor_user_id）</li>
 * <li>いつ（happened_at）</li>
 * <li>何を（target_type, target_id）</li>
 * <li>どのような操作を（action: create|update|delete）</li>
 * <li>変更前後のデータ（before_data, after_data）</li>
 * </ul>
 *
 * <h3>用途</h3>
 * <ul>
 * <li>データ変更履歴の追跡</li>
 * <li>監査証跡の保持</li>
 * <li>問題発生時の原因調査</li>
 * <li>コンプライアンス要件への対応</li>
 * </ul>
 *
 * <h3>マルチテナント分離</h3>
 * <p>
 * company_idによってログが分離され、各会社は自社の監査ログのみを参照できます。
 * </p>
 *
 * @see com.auratime.service.AuditLogService
 */
@Entity
@Table(name = "h_audit_logs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    /**
     * 監査ログID（主キー）
     * UUID v7形式で自動生成されます
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    /**
     * 会社ID
     * マルチテナント分離のため、どの会社のデータ変更かを記録
     */
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    /**
     * 実行者ユーザーID
     * データ変更を実行したユーザーのID
     */
    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    /**
     * 操作種別
     * 値: "create"（作成）| "update"（更新）| "delete"（削除）
     */
    @Column(name = "action", nullable = false)
    private String action;

    /**
     * 対象テーブル名
     * 変更されたエンティティのテーブル名（例: "m_users", "m_employees"）
     */
    @Column(name = "target_type", nullable = false)
    private String targetType;

    /**
     * 対象ID
     * 変更されたエンティティのID
     */
    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    /**
     * 変更前データ
     * JSON形式で保存（PostgreSQLのjsonb型）
     * update操作の場合のみ記録されます
     */
    @Column(name = "before_data", columnDefinition = "jsonb")
    private String beforeData;

    /**
     * 変更後データ
     * JSON形式で保存（PostgreSQLのjsonb型）
     * create操作とupdate操作で記録されます
     */
    @Column(name = "after_data", columnDefinition = "jsonb")
    private String afterData;

    /**
     * リクエストID
     * HTTPリクエストのX-Request-Idヘッダーの値
     * 同じリクエスト内の複数の操作を追跡するために使用
     */
    @Column(name = "request_id")
    private String requestId;

    /**
     * 発生日時
     * データ変更が実際に発生した日時
     * 監査ログの時系列順序を保つために使用
     */
    @Column(name = "happened_at", nullable = false)
    private OffsetDateTime happenedAt;

    // ============================================================================
    // 監査列（自動設定）
    // ============================================================================

    /**
     * 作成日時
     * エンティティ作成時に自動設定されます
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * 作成者ユーザーID
     * AuditorAwareImplにより自動設定されます
     */
    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    /**
     * 更新日時
     * エンティティ更新時に自動更新されます（データベーストリガー）
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 更新者ユーザーID
     * AuditorAwareImplにより自動設定されます
     */
    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    // ============================================================================
    // ソフト削除
    // ============================================================================

    /**
     * 削除日時
     * nullの場合は有効なレコード、値が設定されている場合は削除済み
     * 監査ログは通常削除されませんが、長期保存ポリシーに応じてアーカイブ可能
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * 削除者ユーザーID
     * ソフト削除を実行したユーザーのID
     */
    @Column(name = "deleted_by")
    private UUID deletedBy;
}
