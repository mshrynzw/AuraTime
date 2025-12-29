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
 * 従業員エンティティ
 *
 * <p>
 * 特定の会社との雇用関係を示すエンティティです。
 * ユーザー（m_users）と従業員（m_employees）は分離されており、
 * 1つのユーザーが複数の会社に所属可能です。
 * </p>
 *
 * <h3>関連エンティティ</h3>
 * <ul>
 * <li>{@link User}: ログイン用のユーザー（紐付け可能）</li>
 * <li>{@link Company}: 所属する会社</li>
 * </ul>
 *
 * @see User
 * @see Company
 */
@Entity
@Table(name = "m_employees")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    /**
     * 従業員ID（主キー）
     * UUID v7形式で自動生成されます
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    /**
     * 会社ID
     */
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /**
     * ユーザーID（ログイン紐付け）
     * NULLの場合は未紐付け
     */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * 社員番号
     * 会社内で一意
     */
    @Column(name = "employee_no", nullable = false)
    private String employeeNo;

    /**
     * 雇用区分
     * 値: "fulltime" | "parttime" | "contract"
     * デフォルト: "fulltime"
     */
    @Column(name = "employment_type", nullable = false)
    @Builder.Default
    private String employmentType = "fulltime";

    /**
     * 入社日
     * デフォルト: 現在日付
     */
    @Column(name = "hire_date", nullable = false)
    @Builder.Default
    private LocalDate hireDate = LocalDate.now();

    /**
     * 退職日
     * NULLの場合は在職中
     */
    @Column(name = "termination_date")
    private LocalDate terminationDate;

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
}

