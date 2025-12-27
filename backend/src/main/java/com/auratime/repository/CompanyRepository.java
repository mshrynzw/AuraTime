package com.auratime.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auratime.domain.Company;

/**
 * 会社リポジトリ
 *
 * <p>
 * 会社エンティティに対するデータアクセス操作を提供するリポジトリインターフェースです。
 * ソフト削除を考慮したクエリメソッドを提供します。
 * </p>
 *
 * <h3>ソフト削除対応</h3>
 * <p>
 * すべてのクエリメソッドは、deletedAtがnullのレコード（有効なレコード）のみを対象とします。
 * これにより、削除済み会社が検索結果に含まれることを防ぎます。
 * </p>
 *
 * <h3>マルチテナント対応</h3>
 * <p>
 * 会社はマルチテナント型システムにおけるテナント（データ分離の単位）です。
 * 各会社は独立したデータ空間を持ち、company_idによって厳密に分離されます。
 * </p>
 *
 * @see com.auratime.domain.Company
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    /**
     * IDで会社を検索（削除済みを除く）
     *
     * <p>
     * 指定されたIDに一致する有効な会社を検索します。
     * 会社情報の取得等で使用されます。
     * </p>
     *
     * @param id 検索する会社ID
     * @return 見つかった会社（存在しない場合は空）
     */
    @Query("SELECT c FROM Company c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Company> findByIdAndDeletedAtIsNull(@Param("id") UUID id);

    /**
     * 会社コードで会社を検索（削除済みを除く）
     *
     * <p>
     * 指定された会社コードに一致する有効な会社を検索します。
     * 会社コードは一意の識別子として使用されます。
     * </p>
     *
     * @param code 検索する会社コード
     * @return 見つかった会社（存在しない場合は空）
     */
    @Query("SELECT c FROM Company c WHERE c.code = :code AND c.deletedAt IS NULL")
    Optional<Company> findByCodeAndDeletedAtIsNull(@Param("code") String code);
}
