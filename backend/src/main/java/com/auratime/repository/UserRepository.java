package com.auratime.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auratime.domain.User;

/**
 * ユーザーリポジトリ
 *
 * <p>
 * ユーザーエンティティに対するデータアクセス操作を提供するリポジトリインターフェースです。
 * ソフト削除を考慮したクエリメソッドを提供します。
 * </p>
 *
 * <h3>ソフト削除対応</h3>
 * <p>
 * すべてのクエリメソッドは、deletedAtがnullのレコード（有効なレコード）のみを対象とします。
 * これにより、削除済みユーザーが検索結果に含まれることを防ぎます。
 * </p>
 *
 * @see com.auratime.domain.User
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * メールアドレスでユーザーを検索（削除済みを除く）
     *
     * <p>
     * 指定されたメールアドレスに一致する有効なユーザーを検索します。
     * ログイン処理やユーザー登録時の重複チェックで使用されます。
     * </p>
     *
     * @param email 検索するメールアドレス
     * @return 見つかったユーザー（存在しない場合は空）
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmailAndDeletedAtIsNull(@Param("email") String email);

    /**
     * IDでユーザーを検索（削除済みを除く）
     *
     * <p>
     * 指定されたIDに一致する有効なユーザーを検索します。
     * 現在のユーザー情報取得等で使用されます。
     * </p>
     *
     * @param id 検索するユーザーID
     * @return 見つかったユーザー（存在しない場合は空）
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findByIdAndDeletedAtIsNull(@Param("id") UUID id);
}
