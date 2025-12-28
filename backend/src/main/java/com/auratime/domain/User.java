package com.auratime.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
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
 * ユーザーエンティティ
 *
 * <p>
 * システムにログインするユーザー情報を表すエンティティです。
 * 認証情報（メールアドレス、パスワードハッシュ）と個人情報（氏名等）を保持します。
 * </p>
 *
 * <h3>重要な注意事項</h3>
 * <ul>
 * <li>パスワードは平文ではなく、ハッシュ化された値（password_hash）を保存します</li>
 * <li>1つのユーザーが複数の会社に所属可能（CompanyMembershipで管理）</li>
 * <li>ソフト削除に対応（deleted_atがnullのレコードのみ有効）</li>
 * </ul>
 *
 * <h3>関連エンティティ</h3>
 * <ul>
 * <li>{@link CompanyMembership}: ユーザーと会社の関係（権限情報を含む）</li>
 * <li>{@link Company}: 所属する会社（複数可能）</li>
 * </ul>
 *
 * @see CompanyMembership
 * @see Company
 */
@Entity
@Table(name = "m_users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * ユーザーID（主キー）
     * UUID v7形式で自動生成されます
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    /**
     * メールアドレス
     * ログイン時に使用される一意の識別子
     */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * パスワードハッシュ
     * bcrypt等でハッシュ化されたパスワードを保存
     * 平文での保存は厳禁
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * 姓
     */
    @Column(name = "family_name", nullable = false)
    private String familyName;

    /**
     * 名
     */
    @Column(name = "first_name", nullable = false)
    private String firstName;

    /**
     * 姓（カナ）
     * 任意項目
     */
    @Column(name = "family_name_kana")
    private String familyNameKana;

    /**
     * 名（カナ）
     * 任意項目
     */
    @Column(name = "first_name_kana")
    private String firstNameKana;

    /**
     * ユーザーステータス
     * 値: "active"（有効）| "inactive"（無効）| "locked"（ロック）
     * デフォルト: "active"
     */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "active";

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
     * AuthServiceで手動設定されます（システムボット作成時は自分自身のID）
     * 注意: システムボット作成時に自分自身のIDに更新するため、updatable = falseは設定していません
     */
    @Column(name = "created_by", nullable = false)
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
     * AuthServiceで手動設定されます（システムボット作成時は自分自身のID）
     */
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
