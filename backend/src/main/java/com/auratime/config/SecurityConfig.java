package com.auratime.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.auratime.filter.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security設定クラス
 *
 * <p>
 * アプリケーション全体のセキュリティ設定を定義するクラスです。
 * JWT認証、CORS、パスワードエンコーディングなどの設定を行います。
 * </p>
 *
 * <h3>主な設定内容</h3>
 * <ul>
 * <li>JWT認証フィルターの登録</li>
 * <li>認証不要エンドポイントの設定（/register, /login）</li>
 * <li>CORS設定（フロントエンドとの通信を許可）</li>
 * <li>セッション管理（ステートレス）</li>
 * <li>パスワードエンコーダー（BCrypt）</li>
 * <li>メソッドレベルの認可（@PreAuthorize等）</li>
 * </ul>
 *
 * <h3>認証方式</h3>
 * <p>
 * JWT（JSON Web Token）ベースのステートレス認証を採用しています。
 * セッションを使用せず、各リクエストにJWTトークンを含めることで認証を行います。
 * </p>
 *
 * @see com.auratime.filter.JwtAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** JWT認証フィルター */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * セキュリティフィルターチェーンの設定
     *
     * <p>
     * Spring Securityのフィルターチェーンを設定し、認証・認可のルールを定義します。
     * </p>
     *
     * <h3>設定内容</h3>
     * <ul>
     * <li><strong>CSRF無効化</strong>: RESTful APIのため、CSRF保護を無効化</li>
     * <li><strong>CORS設定</strong>: フロントエンド（localhost:3000）からのリクエストを許可</li>
     * <li><strong>セッション管理</strong>: ステートレス（STATELESS）に設定</li>
     * <li><strong>認証ルール</strong>:
     * <ul>
     * <li>/v1/auth/register, /v1/auth/login: 認証不要（permitAll）</li>
     * <li>その他のエンドポイント: 認証必須（authenticated）</li>
     * </ul>
     * </li>
     * <li><strong>JWTフィルター</strong>: UsernamePasswordAuthenticationFilterの前に配置</li>
     * </ul>
     *
     * @param http HttpSecurityオブジェクト
     * @return 設定済みのSecurityFilterChain
     * @throws Exception 設定エラー
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF保護を無効化（RESTful APIのため）
                .csrf(csrf -> csrf.disable())
                // CORS設定を適用
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // セッション管理をステートレスに設定（JWT認証のため）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 認証ルールの設定
                .authorizeHttpRequests(auth -> auth
                        // 認証不要なエンドポイント
                        .requestMatchers("/v1/auth/register", "/v1/auth/login").permitAll()
                        // その他のエンドポイントは認証必須
                        .anyRequest().authenticated())
                // 認証エラー時に401 Unauthorizedを返すように設定
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint()))
                // JWT認証フィルターをUsernamePasswordAuthenticationFilterの前に配置
                // これにより、すべてのリクエストでJWTトークンが検証されます
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS設定
     *
     * <p>
     * フロントエンドアプリケーション（Next.js）からのリクエストを許可するための設定です。
     * </p>
     *
     * <h3>設定内容</h3>
     * <ul>
     * <li><strong>許可オリジン</strong>: http://localhost:3000（開発環境）</li>
     * <li><strong>許可メソッド</strong>: GET, POST, PUT, PATCH, DELETE, OPTIONS</li>
     * <li><strong>許可ヘッダー</strong>: すべて（*）</li>
     * <li><strong>認証情報</strong>: Cookie等の認証情報を含むリクエストを許可</li>
     * <li><strong>プリフライトリクエストのキャッシュ時間</strong>: 3600秒（1時間）</li>
     * </ul>
     *
     * <h3>本番環境での注意</h3>
     * <p>
     * 本番環境では、許可オリジンを実際のフロントエンドのドメインに変更する必要があります。
     * 環境変数や設定ファイルから読み込むことを推奨します。
     * </p>
     *
     * @return CORS設定ソース
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 許可するオリジン（フロントエンドのURL）
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        // 許可するHTTPメソッド
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // 許可するヘッダー（すべて許可）
        configuration.setAllowedHeaders(List.of("*"));
        // 認証情報（Cookie等）を含むリクエストを許可
        configuration.setAllowCredentials(true);
        // プリフライトリクエストのキャッシュ時間（秒）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // すべてのパスにCORS設定を適用
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * パスワードエンコーダー
     *
     * <p>
     * パスワードのハッシュ化に使用するエンコーダーです。
     * BCryptアルゴリズムを使用してパスワードを安全にハッシュ化します。
     * </p>
     *
     * <h3>BCryptの特徴</h3>
     * <ul>
     * <li>ソルト（ランダムな値）を自動生成し、同じパスワードでも異なるハッシュを生成</li>
     * <li>計算コストを調整可能（デフォルトは10ラウンド）</li>
     * <li>レインボーテーブル攻撃に対して強い</li>
     * </ul>
     *
     * <h3>使用方法</h3>
     * <ul>
     * <li>パスワードのハッシュ化: <code>passwordEncoder.encode(rawPassword)</code></li>
     * <li>パスワードの検証:
     * <code>passwordEncoder.matches(rawPassword, encodedPassword)</code></li>
     * </ul>
     *
     * @return BCryptパスワードエンコーダー
     * @see org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 認証エントリーポイント
     *
     * <p>
     * 認証に失敗した場合（認証情報がない、または無効な場合）に401 Unauthorizedを返すように設定します。
     * デフォルトでは403 Forbiddenが返されますが、RESTful APIの標準に従って401を返します。
     * </p>
     *
     * @return 認証エントリーポイント
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"認証が必要です\"}}");
        };
    }

    /**
     * 認証マネージャー
     *
     * <p>
     * 認証処理を管理するAuthenticationManagerを提供します。
     * 現在はJWT認証のみを使用していますが、将来的にフォーム認証等を追加する場合に使用します。
     * </p>
     *
     * <h3>注意事項</h3>
     * <p>
     * 現在の実装では、JWT認証フィルターで直接認証を行っているため、
     * このAuthenticationManagerは主に将来の拡張のために用意されています。
     * </p>
     *
     * @param config 認証設定
     * @return 認証マネージャー
     * @throws Exception 設定エラー
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
