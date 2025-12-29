package com.auratime.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auratime.domain.UserInvitation;

/**
 * ユーザー招待リポジトリ
 *
 * <p>
 * ユーザー招待エンティティに対するデータアクセス操作を提供するリポジトリインターフェースです。
 * </p>
 *
 * @see com.auratime.domain.UserInvitation
 */
@Repository
public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {

    /**
     * トークンで招待を検索（有効なもののみ）
     *
     * @param token 招待トークン
     * @return 見つかった招待（存在しない場合は空）
     */
    @Query("SELECT i FROM UserInvitation i WHERE i.token = :token AND i.deletedAt IS NULL AND i.status = 'pending'")
    Optional<UserInvitation> findByTokenAndDeletedAtIsNullAndStatusPending(@Param("token") String token);

    /**
     * 会社IDとメールアドレスで招待を検索（有効なもののみ）
     *
     * @param companyId 会社ID
     * @param email メールアドレス
     * @return 見つかった招待（存在しない場合は空）
     */
    @Query("SELECT i FROM UserInvitation i WHERE i.company.id = :companyId AND i.email = :email AND i.deletedAt IS NULL")
    Optional<UserInvitation> findByCompanyIdAndEmailAndDeletedAtIsNull(
            @Param("companyId") UUID companyId,
            @Param("email") String email);
}

