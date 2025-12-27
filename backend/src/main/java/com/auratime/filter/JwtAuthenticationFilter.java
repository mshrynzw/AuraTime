package com.auratime.filter;

import java.io.IOException;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auratime.util.CompanyContext;
import com.auratime.util.JwtTokenProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * JWT認証フィルター
 *
 * <p>
 * すべてのHTTPリクエストに対してJWTトークンを検証し、認証情報を設定するフィルターです。
 * Spring Securityのフィルターチェーンに組み込まれ、コントローラーに到達する前に実行されます。
 * </p>
 *
 * <h3>主な処理</h3>
 * <ul>
 * <li>AuthorizationヘッダーからJWTトークンを抽出</li>
 * <li>トークンの有効性を検証</li>
 * <li>トークンからユーザーID、会社ID、ロールを取得</li>
 * <li>Spring SecurityのSecurityContextに認証情報を設定</li>
 * <li>マルチテナント分離のため、CompanyContextにcompany_idを設定</li>
 * <li>リクエスト処理後にCompanyContextをクリア</li>
 * </ul>
 *
 * <h3>マルチテナント分離</h3>
 * <p>
 * このフィルターは、JWTトークンから取得したcompany_idをCompanyContextに設定します。
 * これにより、Service層やRepository層で自動的にcompany_idによるデータ分離が行われます。
 * </p>
 *
 * @see com.auratime.util.JwtTokenProvider
 * @see com.auratime.util.CompanyContext
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWTトークンプロバイダー */
    private final JwtTokenProvider jwtTokenProvider;

    /** Authorizationヘッダー名 */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearerトークンのプレフィックス */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * フィルターのメイン処理
     *
     * <p>
     * リクエストごとに1回だけ実行されます（OncePerRequestFilterの特性）。
     * </p>
     *
     * <h3>処理フロー</h3>
     * <ol>
     * <li>AuthorizationヘッダーからJWTトークンを抽出</li>
     * <li>トークンが存在し、有効な場合：
     * <ul>
     * <li>トークンからユーザーID、会社ID、ロールを取得</li>
     * <li>SecurityContextに認証情報を設定</li>
     * <li>CompanyContextにcompany_idを設定（マルチテナント分離）</li>
     * </ul>
     * </li>
     * <li>次のフィルターまたはコントローラーに処理を渡す</li>
     * <li>リクエスト処理完了後、CompanyContextをクリア</li>
     * </ol>
     *
     * <h3>エラーハンドリング</h3>
     * <p>
     * トークンの検証や抽出でエラーが発生した場合でも、処理は続行されます。
     * これにより、認証が不要なエンドポイント（/login、/register等）でも正常に動作します。
     * </p>
     *
     * @param request     HTTPリクエスト
     * @param response    HTTPレスポンス
     * @param filterChain フィルターチェーン
     * @throws ServletException サーブレット例外
     * @throws IOException      IO例外
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // AuthorizationヘッダーからJWTトークンを抽出
            String token = extractToken(request);

            // トークンが存在し、有効な場合のみ認証情報を設定
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                // トークンからユーザー情報を取得
                UUID userId = jwtTokenProvider.getUserIdFromToken(token);
                UUID companyId = jwtTokenProvider.getCompanyIdFromToken(token);
                // ロール情報も取得（将来の認可チェックで使用可能）
                jwtTokenProvider.getRoleFromToken(token);

                // Spring SecurityのSecurityContextに認証情報を設定
                // これにより、コントローラーでAuthenticationオブジェクトからユーザーIDを取得可能
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, null);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // マルチテナント分離のため、company_idをThreadLocalに設定
                // Service層やRepository層でCompanyContext.getCompanyId()を使用して
                // 自社データのみにアクセスできるようにする
                CompanyContext.setCompanyId(companyId);
            }
        } catch (Exception e) {
            // トークンの検証や抽出でエラーが発生した場合でも処理を続行
            // 認証が不要なエンドポイントでも正常に動作するようにする
            logger.error("Could not set user authentication in security context", e);
        }

        try {
            // 次のフィルターまたはコントローラーに処理を渡す
            filterChain.doFilter(request, response);
        } finally {
            // リクエスト処理完了後、ThreadLocalをクリアしてメモリリークを防止
            // スレッドプールでスレッドが再利用されるため、クリアは必須
            CompanyContext.clear();
        }
    }

    /**
     * AuthorizationヘッダーからJWTトークンを抽出
     *
     * <p>
     * Authorizationヘッダーの形式: "Bearer {token}"
     * </p>
     *
     * <h3>処理内容</h3>
     * <ul>
     * <li>Authorizationヘッダーを取得</li>
     * <li>"Bearer "プレフィックスで始まるかチェック</li>
     * <li>プレフィックスを除去してトークン部分のみを返却</li>
     * </ul>
     *
     * @param request HTTPリクエスト
     * @return JWTトークン（存在しない場合はnull）
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        // "Bearer {token}"形式かチェック
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            // "Bearer "を除去してトークン部分のみを返却
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
