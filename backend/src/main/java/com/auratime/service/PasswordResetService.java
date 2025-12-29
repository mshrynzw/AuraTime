package com.auratime.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.auratime.api.v1.dto.PasswordResetConfirmRequest;
import com.auratime.api.v1.dto.PasswordResetRequestRequest;
import com.auratime.domain.PasswordResetToken;
import com.auratime.domain.User;
import com.auratime.repository.PasswordResetTokenRepository;
import com.auratime.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * パスワードリセットサービス
 *
 * <p>
 * パスワードリセットに関するビジネスロジックを実装するサービスクラスです。
 * パスワードリセット要求、トークン検証、パスワード更新を提供します。
 * </p>
 *
 * @author AuraTime Development Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * パスワードリセット要求
     *
     * <p>
     * メールアドレスからユーザーを検索し、パスワードリセットトークンを発行します。
     * セキュリティ上の理由から、ユーザーが存在しない場合でも成功レスポンスを返します。
     * </p>
     *
     * @param request パスワードリセット要求リクエスト
     */
    public void requestPasswordReset(PasswordResetRequestRequest request) {
        log.info("Password reset request: email={}", request.getEmail());

        // ユーザーを検索（削除済みを除く）
        userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .ifPresent(user -> {
                    // トークンを生成（UUID v4を使用、推測困難）
                    String token = UUID.randomUUID().toString();

                    // 有効期限を設定（24時間）
                    OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(24);

                    // 既存の有効なトークンを無効化（使用済みにマーク）
                    tokenRepository.findLatestValidByUserId(user.getId())
                            .ifPresent(existingToken -> {
                                existingToken.setUsedAt(OffsetDateTime.now());
                                tokenRepository.save(existingToken);
                            });

                    // 新しいトークンを作成
                    PasswordResetToken resetToken = PasswordResetToken.builder()
                            .user(user)
                            .token(token)
                            .expiresAt(expiresAt)
                            .build();

                    tokenRepository.save(resetToken);

                    log.info("Password reset token created: userId={}, token={}, expiresAt={}",
                            user.getId(), token, expiresAt);

                    // TODO: メール送信機能を実装
                    // emailService.sendPasswordResetEmail(user.getEmail(), token);
                });

        // セキュリティ上の理由から、ユーザーが存在しない場合でも成功レスポンスを返す
        // これにより、メールアドレスの存在確認を防ぐ
    }

    /**
     * パスワードリセット実行
     *
     * <p>
     * リセットトークンを使用してパスワードを更新します。
     * トークンは1回のみ使用可能です。
     * </p>
     *
     * @param request パスワードリセット実行リクエスト
     * @throws ResponseStatusException トークンが無効、期限切れ、使用済みの場合
     */
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        log.info("Password reset confirm: token={}", request.getToken());

        // トークンを検索（未使用のもののみ）
        @SuppressWarnings("null")
        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedAtIsNull(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "リセットトークンが見つかりません"));

        // 有効性チェック
        if (!resetToken.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "リセットトークンが無効または期限切れです");
        }

        // パスワードをハッシュ化
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());

        // ユーザーのパスワードを更新
        User user = resetToken.getUser();
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);

        // トークンを使用済みにマーク
        resetToken.setUsedAt(OffsetDateTime.now());
        tokenRepository.save(resetToken);

        log.info("Password reset completed: userId={}", user.getId());
    }
}

