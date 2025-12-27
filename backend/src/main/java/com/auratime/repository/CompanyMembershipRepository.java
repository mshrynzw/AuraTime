package com.auratime.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auratime.domain.CompanyMembership;

/**
 * 会社所属リポジトリ
 *
 * <p>
 * 会社所属エンティティ（ユーザーと会社の関係・権限）に対するデータアクセス操作を提供するリポジトリインターフェースです。
 * ソフト削除を考慮したクエリメソッドを提供します。
 * </p>
 *
 * <h3>ソフト削除対応</h3>
 * <p>
 * すべてのクエリメソッドは、deletedAtがnullのレコード（有効なレコード）のみを対象とします。
 * これにより、削除済みの所属関係が検索結果に含まれることを防ぎます。
 * </p>
 *
 * <h3>マルチテナント対応</h3>
 * <p>
 * ユーザーは複数の会社に所属可能ですが、各リクエストでは1つの会社IDのみが有効です。
 * このリポジトリは、ユーザーと会社の関係を管理し、ログイン時の会社選択や権限確認に使用されます。
 * </p>
 *
 * @see com.auratime.domain.CompanyMembership
 */
@Repository
public interface CompanyMembershipRepository extends JpaRepository<CompanyMembership, UUID> {

    /**
     * ユーザーIDと会社IDで会社所属を検索（削除済みを除く）
     *
     * <p>
     * 指定されたユーザーが指定された会社に所属しているかを確認します。
     * 現在のユーザー情報取得時や、特定の会社への所属確認で使用されます。
     * </p>
     *
     * <h3>使用例</h3>
     * <ul>
     * <li>現在のユーザーが特定の会社に所属しているか確認</li>
     * <li>マルチテナント分離の検証</li>
     * </ul>
     *
     * @param userId    検索するユーザーID
     * @param companyId 検索する会社ID
     * @return 見つかった会社所属（存在しない場合は空）
     */
    @Query("SELECT cm FROM CompanyMembership cm " +
            "WHERE cm.userId = :userId " +
            "AND cm.companyId = :companyId " +
            "AND cm.deletedAt IS NULL")
    Optional<CompanyMembership> findByUserIdAndCompanyIdAndDeletedAtIsNull(
            @Param("userId") UUID userId,
            @Param("companyId") UUID companyId);

    /**
     * ユーザーIDで会社所属一覧を取得（削除済みを除く）
     *
     * <p>
     * 指定されたユーザーが所属しているすべての会社を取得します。
     * ログイン時に最初の会社を選択する際に使用されます。
     * </p>
     *
     * <h3>使用例</h3>
     * <ul>
     * <li>ログイン時の会社選択（最初の会社を使用）</li>
     * <li>ユーザーが所属する会社一覧の表示</li>
     * </ul>
     *
     * @param userId 検索するユーザーID
     * @return ユーザーが所属する会社所属のリスト
     */
    @Query("SELECT cm FROM CompanyMembership cm " +
            "WHERE cm.userId = :userId " +
            "AND cm.deletedAt IS NULL")
    List<CompanyMembership> findByUserIdAndDeletedAtIsNull(@Param("userId") UUID userId);

    /**
     * 会社IDで会社所属一覧を取得（削除済みを除く）
     *
     * <p>
     * 指定された会社に所属しているすべてのユーザーを取得します。
     * 会社のメンバー一覧表示等で使用されます。
     * </p>
     *
     * <h3>使用例</h3>
     * <ul>
     * <li>会社のメンバー一覧表示</li>
     * <li>会社に所属するユーザーの権限管理</li>
     * </ul>
     *
     * @param companyId 検索する会社ID
     * @return 会社に所属するユーザーの会社所属のリスト
     */
    @Query("SELECT cm FROM CompanyMembership cm " +
            "WHERE cm.companyId = :companyId " +
            "AND cm.deletedAt IS NULL")
    List<CompanyMembership> findByCompanyIdAndDeletedAtIsNull(@Param("companyId") UUID companyId);
}
