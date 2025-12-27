package com.auratime.config;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.stereotype.Component;

/**
 * OffsetDateTimeを提供するDateTimeProvider
 *
 * <p>
 * Spring Data JPAの監査機能（@CreatedDate、@LastModifiedDate）で使用される、
 * 日時を提供するクラスです。
 * </p>
 *
 * <h3>動作仕様</h3>
 * <ul>
 * <li>OffsetDateTimeを返却します</li>
 * <li>エンティティの@CreatedDate、@LastModifiedDateアノテーションが付与された
 * OffsetDateTime型のフィールドに自動的に現在の日時が設定されます</li>
 * </ul>
 *
 * <h3>使用箇所</h3>
 * <p>
 * @EnableJpaAuditingアノテーションのdateTimeProviderRef属性で指定されます。
 * </p>
 *
 * @see org.springframework.data.auditing.DateTimeProvider
 * @see org.springframework.data.annotation.CreatedDate
 * @see org.springframework.data.annotation.LastModifiedDate
 */
@Component("offsetDateTimeProvider")
public class OffsetDateTimeProvider implements DateTimeProvider {

    /**
     * 現在の日時をOffsetDateTimeとして取得
     *
     * <p>
     * Spring Data JPAのAuditing機能から呼び出され、
     * エンティティの作成日時・更新日時を設定するために使用されます。
     * </p>
     *
     * @return 現在の日時（OffsetDateTime）
     */
    @Override
    public Optional<TemporalAccessor> getNow() {
        return Optional.of(OffsetDateTime.now());
    }
}

