package com.auratime.service;

import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auratime.api.v1.dto.LoginRequest;
import com.auratime.api.v1.dto.LoginResponse;
import com.auratime.api.v1.dto.MeResponse;
import com.auratime.api.v1.dto.RegisterRequest;
import com.auratime.domain.Company;
import com.auratime.domain.CompanyMembership;
import com.auratime.domain.User;
import com.auratime.repository.CompanyMembershipRepository;
import com.auratime.repository.CompanyRepository;
import com.auratime.repository.UserRepository;
import com.auratime.util.CompanyContext;
import com.auratime.util.JwtTokenProvider;
import com.auratime.util.SystemConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 認証サービス
 *
 * <p>
 * ユーザー認証に関するビジネスロジックを実装するサービスクラスです。
 * ユーザー登録、ログイン、現在のユーザー情報取得を提供します。
 * </p>
 *
 * <h3>主な機能</h3>
 * <ul>
 * <li>ユーザー登録（メールアドレス重複チェック、パスワードハッシュ化）</li>
 * <li>ログイン（認証情報検証、JWTトークン発行）</li>
 * <li>現在のユーザー情報取得（認証済みユーザーの情報を返却）</li>
 * </ul>
 *
 * <h3>セキュリティ</h3>
 * <ul>
 * <li>パスワードはBCryptでハッシュ化して保存</li>
 * <li>ログイン時はハッシュ化されたパスワードと比較</li>
 * <li>ユーザーステータス（active/inactive/locked）をチェック</li>
 * <li>JWTトークンにユーザーID、会社ID、ロールを含める</li>
 * </ul>
 *
 * <h3>マルチテナント対応</h3>
 * <p>
 * ログイン時は、ユーザーが所属する最初の会社の情報をJWTトークンに含めます。
 * 現在のユーザー情報取得時は、CompanyContextから会社IDを取得して検証します。
 * </p>
 *
 * @see com.auratime.util.JwtTokenProvider
 * @see com.auratime.util.CompanyContext
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    /** ユーザーリポジトリ */
    private final UserRepository userRepository;

    /** 会社所属リポジトリ */
    private final CompanyMembershipRepository companyMembershipRepository;

    /** 会社リポジトリ */
    private final CompanyRepository companyRepository;

    /** パスワードエンコーダー（BCrypt） */
    private final PasswordEncoder passwordEncoder;

    /** JWTトークンプロバイダー */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * ユーザー登録
     *
     * <p>
     * 新規ユーザーを登録します。メールアドレスの重複チェックを行い、
     * パスワードをハッシュ化してからデータベースに保存します。
     * </p>
     *
     * <h3>処理フロー</h3>
     * <ol>
     * <li>メールアドレスの重複チェック（削除済みユーザーを除く）</li>
     * <li>パスワードをBCryptでハッシュ化</li>
     * <li>ユーザーエンティティを作成（ステータスは"active"）</li>
     * <li>データベースに保存（@CreatedBy、@LastModifiedByは自動設定）</li>
     * </ol>
     *
     * <h3>注意事項</h3>
     * <ul>
     * <li>認証されていない状態で呼び出されるため、created_by/updated_byはnullになる可能性があります</li>
     * <li>初期データ投入時は、手動でsystem-botのIDを設定する必要があります</li>
     * </ul>
     *
     * @param request 登録リクエスト（メールアドレス、パスワード、氏名等）
     * @return 登録されたユーザーエンティティ
     * @throws IllegalArgumentException メールアドレスが既に登録されている場合
     */
    public User register(RegisterRequest request) {
        log.info("User registration request: email={}", request.getEmail());

        // メールアドレスの重複チェック（削除済みユーザーを除く）
        if (userRepository.findByEmailAndDeletedAtIsNull(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("このメールアドレスは既に登録されています");
        }

        // パスワードをBCryptでハッシュ化
        // 同じパスワードでも異なるハッシュが生成される（ソルトが自動生成される）
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // システムボットのIDを取得（created_by/updated_by用）
        // システムボットが見つからない場合は作成する
        UUID systemBotId = getOrCreateSystemBot();

        // ユーザーエンティティを作成
        // ステータスは"active"に設定（デフォルト値）
        // created_by/updated_byはシステムボットのIDを設定
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .familyName(request.getFamilyName())
                .firstName(request.getFirstName())
                .familyNameKana(request.getFamilyNameKana())
                .firstNameKana(request.getFirstNameKana())
                .status("active")
                // システムボットのIDを設定（DEFERRABLE制約により、トランザクション内で参照可能）
                .createdBy(systemBotId)
                .updatedBy(systemBotId)
                .build();

        // ユーザーを保存
        @SuppressWarnings("null")
        User savedUser = userRepository.save(user);

        // デフォルトの会社を取得または作成
        Company defaultCompany = getOrCreateDefaultCompany(systemBotId);

        // 会社メンバーシップを作成（デフォルトロール: "employee"）
        CompanyMembership membership = CompanyMembership.builder()
                .companyId(defaultCompany.getId())
                .userId(savedUser.getId())
                .role("employee") // 新規登録ユーザーは従業員ロール
                .joinedAt(java.time.OffsetDateTime.now())
                .createdBy(systemBotId)
                .updatedBy(systemBotId)
                .build();

        @SuppressWarnings("null")
        CompanyMembership savedMembership = companyMembershipRepository.save(membership);

        log.info("User registered successfully: userId={}, email={}, companyId={}, membershipId={}",
                savedUser.getId(), savedUser.getEmail(), defaultCompany.getId(), savedMembership.getId());
        return savedUser;
    }

    /**
     * ユーザーログイン
     *
     * <p>
     * メールアドレスとパスワードでユーザーを認証し、JWTトークンを発行します。
     * 認証に成功した場合、ユーザー情報とJWTトークンを返却します。
     * </p>
     *
     * <h3>処理フロー</h3>
     * <ol>
     * <li>メールアドレスでユーザーを検索（削除済みユーザーを除く）</li>
     * <li>パスワードの検証（BCryptでハッシュ化されたパスワードと比較）</li>
     * <li>ユーザーステータスのチェック（"active"のみ許可）</li>
     * <li>会社メンバーシップを取得（最初の会社を使用）</li>
     * <li>JWTトークンを生成（ユーザーID、会社ID、ロールを含む）</li>
     * <li>ログインレスポンスを返却</li>
     * </ol>
     *
     * <h3>セキュリティ</h3>
     * <ul>
     * <li>ユーザーが見つからない場合とパスワードが一致しない場合、同じエラーメッセージを返却（情報漏洩防止）</li>
     * <li>ステータスが"active"でない場合、ログインを拒否</li>
     * <li>会社に所属していない場合、ログインを拒否</li>
     * </ul>
     *
     * <h3>マルチテナント対応</h3>
     * <p>
     * ユーザーが複数の会社に所属している場合、最初の会社の情報をJWTトークンに含めます。
     * 将来的には、ログイン時に会社を選択する機能を追加することを検討してください。
     * </p>
     *
     * @param request ログインリクエスト（メールアドレス、パスワード）
     * @return ログインレスポンス（JWTトークン、ユーザー情報）
     * @throws BadCredentialsException メールアドレスまたはパスワードが正しくない場合
     * @throws IllegalStateException   アカウントが無効化されている、または会社に所属していない場合
     */
    public LoginResponse login(LoginRequest request) {
        log.info("Login request: email={}", request.getEmail());

        // メールアドレスでユーザーを検索（削除済みユーザーを除く）
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found, email={}", request.getEmail());
                    return new BadCredentialsException("メールアドレスまたはパスワードが正しくありません");
                });

        // パスワードの検証
        // BCryptのmatchesメソッドで、平文パスワードとハッシュ化されたパスワードを比較
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: password mismatch, email={}", request.getEmail());
            throw new BadCredentialsException("メールアドレスまたはパスワードが正しくありません");
        }

        // ユーザーのステータスチェック
        // "active"以外のステータス（"inactive"、"locked"）の場合はログインを拒否
        if (!"active".equals(user.getStatus())) {
            log.warn("Login failed: account is not active, email={}, status={}", request.getEmail(), user.getStatus());
            throw new IllegalStateException("このアカウントは無効化されています");
        }

        // 会社メンバーシップを取得（最初の会社を使用）
        // ユーザーが複数の会社に所属している場合、最初の会社の情報を使用
        // TODO: 将来的には、ログイン時に会社を選択する機能を追加
        CompanyMembership membership = companyMembershipRepository
                .findByUserIdAndDeletedAtIsNull(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Login failed: user has no company membership, userId={}", user.getId());
                    return new IllegalStateException("ユーザーはどの会社にも所属していません");
                });

        // JWTトークンを生成
        // トークンには以下の情報が含まれます：
        // - ユーザーID（user_id）
        // - 会社ID（company_id）
        // - ロール（role）
        // - 有効期限（24時間）
        String token = jwtTokenProvider.generateToken(
                user.getId(),
                membership.getCompanyId(),
                membership.getRole());

        log.info("Login successful: userId={}, email={}, companyId={}, role={}",
                user.getId(), user.getEmail(), membership.getCompanyId(), membership.getRole());

        return LoginResponse.builder()
                .token(token)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .familyName(user.getFamilyName())
                        .firstName(user.getFirstName())
                        .companyId(membership.getCompanyId())
                        .role(membership.getRole())
                        .build())
                .build();
    }

    /**
     * 現在のユーザー情報取得
     *
     * <p>
     * 認証済みユーザーの情報を取得します。
     * JWTトークンから取得したユーザーIDと会社IDを使用して、ユーザー情報と会社所属情報を取得します。
     * </p>
     *
     * <h3>処理フロー</h3>
     * <ol>
     * <li>AuthenticationオブジェクトからユーザーIDを取得</li>
     * <li>CompanyContextから会社IDを取得（JWTトークンから設定された値）</li>
     * <li>ユーザー情報を取得（削除済みユーザーを除く）</li>
     * <li>会社所属情報を取得（指定された会社への所属を確認）</li>
     * <li>レスポンスを構築して返却</li>
     * </ol>
     *
     * <h3>マルチテナント分離</h3>
     * <p>
     * CompanyContextから取得した会社IDを使用して、ユーザーがその会社に所属していることを確認します。
     * これにより、他社のデータにアクセスできないことを保証します。
     * </p>
     *
     * <h3>トランザクション</h3>
     * <p>
     * 読み取り専用トランザクション（@Transactional(readOnly = true)）を使用して、
     * パフォーマンスを最適化しています。
     * </p>
     *
     * @param authentication Spring Securityの認証情報（PrincipalにユーザーIDが含まれる）
     * @return 現在のユーザー情報（ID、メールアドレス、氏名、会社ID、ロール等）
     * @throws IllegalArgumentException ユーザーが見つからない場合
     * @throws IllegalStateException    会社への所属が見つからない場合
     */
    @Transactional(readOnly = true)
    public MeResponse getCurrentUser(Authentication authentication) {
        // AuthenticationオブジェクトのPrincipalには、JwtAuthenticationFilterで設定されたユーザーID（UUID）が含まれる
        UUID userId = (UUID) authentication.getPrincipal();

        // CompanyContextから会社IDを取得
        // JwtAuthenticationFilterでJWTトークンから取得したcompany_idが設定されている
        UUID companyId = CompanyContext.getCompanyId();

        if (companyId == null) {
            log.warn("Company ID is not set in CompanyContext: userId={}", userId);
            throw new IllegalStateException("会社IDが設定されていません");
        }

        log.debug("Getting current user: userId={}, companyId={}", userId, companyId);

        // ユーザー情報を取得（削除済みユーザーを除く）
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: userId={}", userId);
                    return new IllegalArgumentException("ユーザーが見つかりません");
                });

        // 会社所属情報を取得
        // 指定された会社への所属を確認（マルチテナント分離のため）
        CompanyMembership membership = companyMembershipRepository
                .findByUserIdAndCompanyIdAndDeletedAtIsNull(userId, companyId)
                .orElseThrow(() -> {
                    log.warn("Company membership not found: userId={}, companyId={}", userId, companyId);
                    return new IllegalStateException("会社への所属が見つかりません");
                });

        // レスポンスを構築
        return MeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .familyName(user.getFamilyName())
                .firstName(user.getFirstName())
                .familyNameKana(user.getFamilyNameKana())
                .firstNameKana(user.getFirstNameKana())
                .status(user.getStatus())
                .companyId(companyId)
                .role(membership.getRole())
                .build();
    }

    /**
     * システムボットのIDを取得または作成
     *
     * <p>
     * システムボットのIDを取得します。
     * 存在しない場合は作成します。
     * </p>
     *
     * <h3>処理フロー</h3>
     * <ol>
     * <li>システムボットのメールアドレスで検索</li>
     * <li>見つかった場合はそのIDを返却</li>
     * <li>見つからない場合は作成（created_byは自分自身に設定）</li>
     * </ol>
     *
     * <h3>注意事項</h3>
     * <p>
     * システムボット作成時は、created_by/updated_byをnullに設定することで、
     * Userエンティティの@PrePersistメソッドが一時的なUUIDを設定します。
     * その後、@PostPersistで自分自身のIDに置き換えられます。
     * しかし、@PostPersistは永続化後に実行されるため、データベースには反映されません。
     * そのため、flush()してIDを生成した後、自分自身のIDに設定し、再度save()します。
     * </p>
     *
     * @return システムボットのユーザーID
     */
    private UUID getOrCreateSystemBot() {
        // システムボットを検索
        return userRepository.findByEmailAndDeletedAtIsNull(SystemConstants.SYSTEM_BOT_EMAIL)
                .map(User::getId)
                .orElseGet(() -> {
                    // システムボットが見つからない場合は作成
                    log.info("Creating system bot user: email={}", SystemConstants.SYSTEM_BOT_EMAIL);

                    // システムボットを作成
                    // 最初は一時的なUUIDを設定（NOT NULL制約を満たすため）
                    // flush()後に自分自身のIDに置き換える
                    UUID tempUuid = UUID.randomUUID();
                    User systemBot = User.builder()
                            .email(SystemConstants.SYSTEM_BOT_EMAIL)
                            .passwordHash(passwordEncoder.encode(SystemConstants.SYSTEM_BOT_PASSWORD))
                            .familyName("System")
                            .firstName("Bot")
                            .status("active")
                            // 一時的なUUIDを設定（後で自分自身のIDに置き換える）
                            .createdBy(tempUuid)
                            .updatedBy(tempUuid)
                            .build();

                    // 最初のsave()でIDを生成
                    @SuppressWarnings("null")
                    User savedSystemBot = userRepository.save(systemBot);

                    // flush()してデータベースに反映（IDが生成される）
                    // DEFERRABLE制約により、トランザクション内で自己参照が可能
                    userRepository.flush();

                    // システムボットのcreated_by/updated_byを自分自身のIDに設定
                    UUID systemBotId = savedSystemBot.getId();
                    savedSystemBot.setCreatedBy(systemBotId);
                    savedSystemBot.setUpdatedBy(systemBotId);

                    // 再度save()して更新（DEFERRABLE制約により、トランザクション内で自己参照が可能）
                    @SuppressWarnings("null")
                    User updatedSystemBot = userRepository.save(savedSystemBot);

                    log.info("System bot created: userId={}", updatedSystemBot.getId());
                    return updatedSystemBot.getId();
                });
    }

    /**
     * デフォルトの会社を取得または作成
     *
     * <p>
     * ユーザー登録時に使用するデフォルトの会社を取得します。
     * 存在しない場合は作成します。
     * </p>
     *
     * <h3>処理フロー</h3>
     * <ol>
     * <li>デフォルトの会社コード（"DEFAULT"）で検索</li>
     * <li>見つかった場合はその会社を返却</li>
     * <li>見つからない場合は作成（会社名: "デフォルト会社"）</li>
     * </ol>
     *
     * @param systemBotId システムボットのユーザーID（created_by/updated_by用）
     * @return デフォルトの会社エンティティ
     */
    private Company getOrCreateDefaultCompany(UUID systemBotId) {
        // デフォルトの会社コードで検索
        return companyRepository.findByCodeAndDeletedAtIsNull("DEFAULT")
                .orElseGet(() -> {
                    // デフォルトの会社が見つからない場合は作成
                    log.info("Creating default company: code=DEFAULT");

                    Company defaultCompany = Company.builder()
                            .name("デフォルト会社")
                            .code("DEFAULT")
                            .timezone("Asia/Tokyo")
                            .currency("JPY")
                            .createdBy(systemBotId)
                            .updatedBy(systemBotId)
                            .build();

                    @SuppressWarnings("null")
                    Company savedCompany = companyRepository.save(defaultCompany);

                    log.info("Default company created: companyId={}, code={}", savedCompany.getId(), savedCompany.getCode());
                    return savedCompany;
                });
    }
}
