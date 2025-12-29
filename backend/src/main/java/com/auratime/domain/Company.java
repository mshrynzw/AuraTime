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
 * 会社エンティティ（テナント）
 *
 * <p>
 * マルチテナント型システムにおけるテナント（会社）を表すエンティティです。
 * 各会社は独立したデータ空間を持ち、company_idによって厳密に分離されます。
 * </p>
 *
 * <h3>マルチテナント分離</h3>
 * <p>
 * すべての業務データ（勤怠、給与、従業員等）はcompany_idを持ち、
 * このIDによってデータが分離されます。ユーザーは複数の会社に所属可能ですが、
 * 各リクエストでは1つのcompany_idのみが有効です。
 * </p>
 *
 * <h3>関連エンティティ</h3>
 * <ul>
 * <li>{@link CompanyMembership}: この会社に所属するユーザーと権限</li>
 * <li>{@link User}: この会社に所属するユーザー（複数可能）</li>
 * </ul>
 *
 * @see CompanyMembership
 * @see User
 */
@Entity
@Table(name = "m_companies")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    /**
     * 会社ID（主キー）
     * UUID v7形式で自動生成されます
     * すべての業務データのcompany_idとして使用されます
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    /**
     * 会社名
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * 会社コード
     * 一意の識別子（例: "COMPANY001"）
     * システム内で一意である必要があります
     */
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    /**
     * タイムゾーン
     * IANAタイムゾーンデータベース形式（例: "Asia/Tokyo"）
     * デフォルト: "Asia/Tokyo"
     */
    @Column(name = "timezone", nullable = false)
    @Builder.Default
    private String timezone = "Asia/Tokyo";

    /**
     * 通貨
     * ISO 4217通貨コード（例: "JPY", "USD"）
     * デフォルト: "JPY"
     */
    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "JPY";

    /**
     * 最大ユーザー数
     * NULLの場合は無制限（簡易ライセンス数管理）
     */
    @Column(name = "max_users")
    private Integer maxUsers;

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
