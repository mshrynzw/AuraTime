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
 * 会社所属エンティティ（ユーザーと会社の関係・権限）
 *
 * <p>
 * ユーザーと会社の関係、およびその会社における権限（ロール）を表すエンティティです。
 * 1つのユーザーが複数の会社に所属可能なため、このエンティティで多対多の関係を管理します。
 * </p>
 *
 * <h3>ロール（権限）</h3>
 * <ul>
 * <li><strong>system_admin</strong>: システム管理者（全社の設定管理）</li>
 * <li><strong>admin</strong>: 管理者（自社の全データ管理）</li>
 * <li><strong>manager</strong>: マネージャー（部下の勤怠承認等）</li>
 * <li><strong>employee</strong>: 従業員（自身のデータのみ）</li>
 * </ul>
 *
 * <h3>マルチテナント分離</h3>
 * <p>
 * JWTトークンにはcompany_idとroleが含まれ、この情報に基づいてアクセス制御が行われます。
 * ユーザーが複数の会社に所属している場合、ログイン時に最初の会社が選択されます。
 * </p>
 *
 * <h3>関連エンティティ</h3>
 * <ul>
 * <li>{@link User}: 所属するユーザー</li>
 * <li>{@link Company}: 所属する会社</li>
 * </ul>
 *
 * @see User
 * @see Company
 */
@Entity
@Table(name = "r_company_memberships")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyMembership {

    /**
     * 会社所属ID（主キー）
     * UUID v7形式で自動生成されます
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    /**
     * 会社ID
     * 所属する会社を表します
     */
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    /**
     * ユーザーID
     * 所属するユーザーを表します
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * ロール（権限）
     * 値: "system_admin" | "admin" | "manager" | "employee"
     * この会社におけるユーザーの権限を表します
     */
    @Column(name = "role", nullable = false)
    private String role;

    /**
     * 参加日時
     * ユーザーがこの会社に参加した日時
     * 任意項目
     */
    @Column(name = "joined_at")
    private OffsetDateTime joinedAt;

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
     * ソフト削除により、ユーザーと会社の関係を無効化します
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
