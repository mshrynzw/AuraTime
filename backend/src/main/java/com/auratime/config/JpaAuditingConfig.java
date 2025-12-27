package com.auratime.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA監査設定クラス
 *
 * <p>
 * Spring Data JPAの監査機能（@CreatedBy、@LastModifiedBy、@CreatedDate、@LastModifiedDate）を
 * 有効化する設定クラスです。
 * </p>
 *
 * <h3>設定内容</h3>
 * <ul>
 * <li>@EnableJpaAuditing: JPA監査機能を有効化</li>
 * <li>dateTimeProviderRef: OffsetDateTimeProviderを使用してOffsetDateTimeをサポート</li>
 * </ul>
 *
 * <h3>分離の理由</h3>
 * <p>
 * @WebMvcTestではJPAエンティティがロードされないため、@EnableJpaAuditingをメインクラスに
 * 配置するとテストでエラーが発生します。そのため、別の設定クラスに分離しています。
 * </p>
 *
 * @see com.auratime.config.OffsetDateTimeProvider
 * @see com.auratime.config.AuditorAwareImpl
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class JpaAuditingConfig {
}

