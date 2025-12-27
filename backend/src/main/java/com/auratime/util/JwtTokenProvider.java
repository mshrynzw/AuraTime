package com.auratime.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWTトークンプロバイダー
 *
 * <p>
 * JWT（JSON Web Token）の生成、検証、情報抽出を行うクラスです。
 * 認証・認可の基盤として使用されます。
 * </p>
 *
 * <h3>機能:</h3>
 * <ul>
 * <li>JWTトークンの生成（ユーザーID、会社ID、ロールを含む）</li>
 * <li>JWTトークンの検証</li>
 * <li>JWTトークンからの情報抽出（ユーザーID、会社ID、ロール）</li>
 * </ul>
 *
 * <h3>トークンに含まれる情報:</h3>
 * <ul>
 * <li>{@code subject}: ユーザーID</li>
 * <li>{@code company_id}: 会社ID（マルチテナント分離用）</li>
 * <li>{@code role}: ロール（system_admin, admin, manager, employee）</li>
 * <li>{@code issuedAt}: 発行日時</li>
 * <li>{@code expiration}: 有効期限</li>
 * </ul>
 *
 * <h3>設定:</h3>
 * <ul>
 * <li>{@code jwt.secret}: JWT署名用の秘密鍵（application.yml）</li>
 * <li>{@code jwt.expiration}: トークンの有効期限（ミリ秒、デフォルト: 24時間）</li>
 * </ul>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@Component
public class JwtTokenProvider {

    /** JWT署名用の秘密鍵（application.ymlから読み込み） */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /** トークンの有効期限（ミリ秒、application.ymlから読み込み） */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * 署名用の秘密鍵を取得
     *
     * <p>
     * JWT署名に使用するHMAC-SHA鍵を生成します。
     * </p>
     *
     * @return 署名用の秘密鍵
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JWTトークンを生成
     *
     * <p>
     * ユーザーID、会社ID、ロールを含むJWTトークンを生成します。
     * 有効期限は設定値（デフォルト: 24時間）に基づきます。
     * </p>
     *
     * @param userId    ユーザーID
     * @param companyId 会社ID（マルチテナント分離用）
     * @param role      ロール（system_admin, admin, manager, employee）
     * @return JWTトークン文字列
     */
    public String generateToken(UUID userId, UUID companyId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("company_id", companyId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * トークンからユーザーIDを取得
     *
     * <p>
     * JWTトークンのsubject（ユーザーID）を取得します。
     * </p>
     *
     * @param token JWTトークン文字列
     * @return ユーザーID
     * @throws io.jsonwebtoken.JwtException トークンが無効な場合
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }

    /**
     * トークンから会社IDを取得
     *
     * <p>
     * JWTトークンに含まれる会社ID（company_idクレーム）を取得します。
     * マルチテナント分離に使用されます。
     * </p>
     *
     * @param token JWTトークン文字列
     * @return 会社ID
     * @throws io.jsonwebtoken.JwtException トークンが無効な場合
     */
    public UUID getCompanyIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.get("company_id", String.class));
    }

    /**
     * トークンからロールを取得
     *
     * <p>
     * JWTトークンに含まれるロール（roleクレーム）を取得します。
     * 認可チェックに使用されます。
     * </p>
     *
     * @param token JWTトークン文字列
     * @return ロール（system_admin, admin, manager, employee）
     * @throws io.jsonwebtoken.JwtException トークンが無効な場合
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }

    /**
     * トークンの有効性を検証
     *
     * <p>
     * JWTトークンの署名、有効期限、形式を検証します。
     * 無効なトークンの場合はfalseを返します。
     * </p>
     *
     * @param token JWTトークン文字列
     * @return トークンが有効な場合true、無効な場合false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
