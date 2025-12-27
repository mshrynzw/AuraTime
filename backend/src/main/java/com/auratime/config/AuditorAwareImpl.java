package com.auratime.config;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 監査情報提供実装クラス
 *
 * <p>
 * Spring Data JPAの監査機能（@CreatedBy、@LastModifiedBy）で使用される、
 * 現在の監査者（ユーザーID）を提供するクラスです。
 * </p>
 *
 * <h3>動作仕様</h3>
 * <ul>
 * <li>Spring SecurityのSecurityContextから認証情報を取得</li>
 * <li>認証済みで、PrincipalがUUIDの場合、そのUUIDを返却</li>
 * <li>認証されていない場合（初期データ投入時など）は空を返却</li>
 * </ul>
 *
 * <h3>使用箇所</h3>
 * <p>
 * エンティティの@CreatedBy、@LastModifiedByアノテーションが付与されたフィールドに
 * 自動的に現在のユーザーIDが設定されます。
 * </p>
 *
 * <h3>注意事項</h3>
 * <ul>
 * <li>認証されていない状態でエンティティを保存すると、created_by/updated_byがnullになる可能性があります</li>
 * <li>初期データ投入時など、認証されていない状態でデータを作成する場合は、手動でユーザーIDを設定する必要があります</li>
 * </ul>
 *
 * @see org.springframework.data.domain.AuditorAware
 * @see org.springframework.data.annotation.CreatedBy
 * @see org.springframework.data.annotation.LastModifiedBy
 */
@Component
public class AuditorAwareImpl implements AuditorAware<UUID> {

    /**
     * 現在の監査者（ユーザーID）を取得
     *
     * <p>
     * Spring SecurityのSecurityContextから認証情報を取得し、
     * 認証済みユーザーのID（Principal）を返却します。
     * </p>
     *
     * <h3>処理フロー</h3>
     * <ol>
     * <li>SecurityContextからAuthenticationオブジェクトを取得</li>
     * <li>認証済みかつPrincipalがUUID型の場合、そのUUIDを返却</li>
     * <li>それ以外の場合は空を返却（システムボットや初期データ投入時など）</li>
     * </ol>
     *
     * <h3>JWT認証との連携</h3>
     * <p>
     * JwtAuthenticationFilterで設定されたAuthenticationオブジェクトのPrincipalには
     * ユーザーID（UUID）が設定されているため、このメソッドで取得可能です。
     * </p>
     *
     * @return 現在のユーザーID（認証されていない場合は空）
     * @see com.auratime.filter.JwtAuthenticationFilter
     */
    @Override
    @NonNull
    @SuppressWarnings("null")
    public Optional<UUID> getCurrentAuditor() {
        // Spring SecurityのSecurityContextから認証情報を取得
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 認証済みで、PrincipalがUUID型の場合、そのUUIDを返却
        // JwtAuthenticationFilterで設定されたPrincipalはユーザーID（UUID）です
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UUID) {
            return Optional.of((UUID) authentication.getPrincipal());
        }

        // 認証されていない場合（初期データ投入時、システムボット実行時など）は空を返却
        // この場合、エンティティのcreated_by/updated_byはnullになる可能性があります
        // Optional.empty()はnullではなく空のOptionalオブジェクトを返すため、@NonNullと互換性があります
        return Optional.empty();
    }
}
