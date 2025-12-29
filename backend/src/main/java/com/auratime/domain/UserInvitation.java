package com.auratime.domain;

import java.time.LocalDate;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ユーザー招待エンティティ
 *
 * <p>
 * 管理者が発行するユーザー招待トークンを管理するエンティティです。
 * 招待トークンを使用して、新規ユーザーまたは既存ユーザーを会社に招待します。
 * </p>
 *
 * <h3>ステータス</h3>
 * <ul>
 * <li><strong>pending</strong>: 未使用（デフォルト）</li>
 * <li><strong>used</strong>: 使用済み</li>
 * <li><strong>expired</strong>: 期限切れ</li>
 * <li><strong>canceled</strong>: 取り消し済み</li>
 * </ul>
 *
 * <h3>関連エンティティ</h3>
 * <ul>
 * <li>{@link Company}: 招待先の会社</li>
 * <li>{@link User}: 使用したユーザー（used_by）</li>
 * </ul>
 *
 * @see Company
 * @see User
 */
@Entity
@Table(name = "t_user_invitations")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInvitation {

    /**
     * 招待ID（主キー）
     * UUID v7形式で自動生成されます
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    /**
     * 会社ID
     * 招待先の会社
     */
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /**
     * 招待先メールアドレス
     */
    @Column(name = "email", nullable = false)
    private String email;

    /**
     * 招待トークン
     * UUID v7形式を推奨（推測困難）
     */
    @Column(name = "token", nullable = false)
    private String token;

    /**
     * 付与するロール
     * 値: "system_admin" | "admin" | "manager" | "employee"
     */
    @Column(name = "role", nullable = false)
    private String role;

    /**
     * 社員番号（必須）
     */
    @Column(name = "employee_no", nullable = false)
    private String employeeNo;

    /**
     * 雇用区分
     * 値: "fulltime" | "parttime" | "contract"
     */
    @Column(name = "employment_type")
    private String employmentType;

    /**
     * 入社日
     */
    @Column(name = "hire_date")
    private LocalDate hireDate;

    /**
     * 有効期限
     */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /**
     * 最大使用回数
     * デフォルト: 1
     */
    @Column(name = "max_uses", nullable = false)
    @Builder.Default
    private Integer maxUses = 1;

    /**
     * 使用回数
     * デフォルト: 0
     */
    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    /**
     * 使用日時
     * 最初に使用された日時
     */
    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    /**
     * 使用したユーザーID
     * 最初に使用したユーザー
     */
    @ManyToOne
    @JoinColumn(name = "used_by")
    private User usedBy;

    /**
     * ステータス
     * 値: "pending" | "used" | "expired" | "canceled"
     * デフォルト: "pending"
     */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "pending";

    // ============================================================================
    // 監査列（自動設定）
    // ============================================================================

    /**
     * 作成日時
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * 作成者ユーザーID
     */
    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    /**
     * 更新日時
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 更新者ユーザーID
     */
    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    // ============================================================================
    // ソフト削除
    // ============================================================================

    /**
     * 削除日時
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * 削除者ユーザーID
     */
    @Column(name = "deleted_by")
    private UUID deletedBy;

    /**
     * 招待トークンが有効かどうかを判定
     *
     * @return 有効な場合true
     */
    public boolean isValid() {
        return "pending".equals(status)
                && expiresAt.isAfter(OffsetDateTime.now())
                && usedCount < maxUses
                && deletedAt == null;
    }
}

