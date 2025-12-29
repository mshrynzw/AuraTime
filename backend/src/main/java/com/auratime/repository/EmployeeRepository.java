package com.auratime.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.auratime.domain.Employee;

/**
 * 従業員リポジトリ
 *
 * <p>
 * 従業員エンティティに対するデータアクセス操作を提供するリポジトリインターフェースです。
 * </p>
 *
 * @see com.auratime.domain.Employee
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    /**
     * 会社IDと社員番号で従業員を検索（削除済みを除く）
     *
     * @param companyId  会社ID
     * @param employeeNo 社員番号
     * @return 見つかった従業員（存在しない場合は空）
     */
    @Query("SELECT e FROM Employee e WHERE e.company.id = :companyId AND e.employeeNo = :employeeNo AND e.deletedAt IS NULL")
    Optional<Employee> findByCompanyIdAndEmployeeNoAndDeletedAtIsNull(
            @Param("companyId") UUID companyId,
            @Param("employeeNo") String employeeNo);
}

