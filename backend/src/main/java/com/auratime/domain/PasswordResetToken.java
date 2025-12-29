package com.auratime.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * パスワードリセットトークンエンティティ
 *
 * <p>
 * パスワードリセット要求時に発行されるトークンを管理するエンティティです。
 * トークンは有効期限内に1回のみ使用可能です。
 * </p>
 *
 * <h3>関連エンティティ</h3>
 * <ul>
 * <li>{@link User}: パスワードをリセットするユーザー</li>
 * </ul>
 *
 * @see User
 */
@Entity
@Table(name = "t_password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    /**
     * トークンID（主キー）
     * UUID v7形式で自動生成されます
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    /**
     * ユーザーID
     * パスワードをリセットするユーザー
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * リセットトークン
     * UUID v7形式を推奨（推測困難）
     */
    @Column(name = "token", nullable = false, unique = true)
    private String token;

    /**
     * 有効期限
     */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /**
     * 使用日時
     * nullの場合は未使用
     */
    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    /**
     * 作成日時
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /**
     * トークンが有効かどうかを判定
     *
     * @return 有効な場合true（未使用かつ有効期限内）
     */
    public boolean isValid() {
        return usedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }
}

