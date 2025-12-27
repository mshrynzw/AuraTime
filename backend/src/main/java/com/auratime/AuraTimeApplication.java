package com.auratime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AuraTimeアプリケーションのメインクラス
 *
 * <p>
 * このクラスはSpring Bootアプリケーションのエントリーポイントです。
 * アプリケーションの起動と各種機能の有効化を行います。
 * </p>
 *
 * <h3>有効化されている機能:</h3>
 * <ul>
 * <li>{@code @SpringBootApplication}: Spring Bootの自動設定とコンポーネントスキャンを有効化</li>
 * <li>{@code @EnableAsync}: 非同期処理を有効化（@Asyncアノテーションの使用を可能にする）</li>
 * </ul>
 *
 * <h3>JPA監査機能について</h3>
 * <p>
 * JPA監査機能（@CreatedBy、@LastModifiedBy等）は、{@link com.auratime.config.JpaAuditingConfig}で
 * 有効化されています。@WebMvcTestでのテストを考慮して、メインクラスから分離しています。
 * </p>
 *
 * @see com.auratime.config.JpaAuditingConfig
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableAsync
public class AuraTimeApplication {

    /**
     * アプリケーションのエントリーポイント
     *
     * <p>
     * Spring Bootアプリケーションを起動します。
     * このメソッドが実行されると、Springコンテキストが初期化され、
     * 設定されたすべてのBeanがロードされます。
     * </p>
     *
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        SpringApplication.run(AuraTimeApplication.class, args);
    }
}
