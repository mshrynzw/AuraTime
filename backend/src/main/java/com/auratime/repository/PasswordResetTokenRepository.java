package com.auratime.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auratime.domain.PasswordResetToken;

/**
 * パスワードリセットトークンリポジトリ
 *
 * <p>
 * パスワードリセットトークンエンティティに対するデータアクセス操作を提供するリポジトリインターフェースです。
 * </p>
 *
 * @see com.auratime.domain.PasswordResetToken
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * トークンでリセットトークンを検索（未使用のもののみ）
     *
     * @param token リセットトークン
     * @return 見つかったトークン（存在しない場合は空）
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.token = :token AND t.usedAt IS NULL")
    Optional<PasswordResetToken> findByTokenAndUsedAtIsNull(@Param("token") String token);

    /**
     * ユーザーIDで有効なリセットトークンを検索（未使用かつ有効期限内）
     *
     * @param userId ユーザーID
     * @return 見つかったトークン（存在しない場合は空）
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.user.id = :userId AND t.usedAt IS NULL AND t.expiresAt > CURRENT_TIMESTAMP ORDER BY t.createdAt DESC")
    Optional<PasswordResetToken> findLatestValidByUserId(@Param("userId") UUID userId);
}

