package com.auratime.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.auratime.api.v1.dto.LoginRequest;
import com.auratime.api.v1.dto.LoginResponse;
import com.auratime.api.v1.dto.MeResponse;
import com.auratime.api.v1.dto.RegisterRequest;
import com.auratime.domain.Company;
import com.auratime.domain.CompanyMembership;
import com.auratime.domain.Employee;
import com.auratime.domain.User;
import com.auratime.domain.UserInvitation;
import com.auratime.repository.CompanyMembershipRepository;
import com.auratime.repository.CompanyRepository;
import com.auratime.repository.EmployeeRepository;
import com.auratime.repository.UserInvitationRepository;
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

    /** 従業員リポジトリ */
    private final EmployeeRepository employeeRepository;

    /** 招待リポジトリ */
    private final UserInvitationRepository invitationRepository;

    /** 招待サービス */
    private final InvitationService invitationService;

    /** パスワードエンコーダー（BCrypt） */
    private final PasswordEncoder passwordEncoder;

    /** JWTトークンプロバイダー */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * ユーザー登録（招待トークン方式）
     *
     * <p>
     * 招待トークンを使用してユーザーを登録します。
     * 既存ユーザーの場合は追加登録、新規ユーザーの場合は新規作成を行います。
     * </p>
     *
     * <h3>処理フロー</h3>
     * <ol>
     * <li>招待トークンの検証（有効性、期限、使用回数）</li>
     * <li>ライセンス数チェック</li>
     * <li>メールアドレスで既存ユーザーを検索</li>
     * <li>既存ユーザーの場合：パスワード不要、会社所属と従業員レコードのみ作成</li>
     * <li>新規ユーザーの場合：パスワードハッシュ化、ユーザー作成、会社所属と従業員レコード作成</li>
     * <li>招待トークンを使用済みにマーク</li>
     * </ol>
     *
     * <h3>注意事項</h3>
     * <ul>
     * <li>招待トークンは必須です</li>
     * <li>既存ユーザーの場合、パスワードは不要です</li>
     * <li>新規ユーザーの場合、パスワード、氏名は必須です</li>
     * </ul>
     *
     * @param request 登録リクエスト（招待トークン、メールアドレス、パスワード、氏名等）
     * @return 登録されたユーザーエンティティ
     * @throws ResponseStatusException 招待トークンが無効、ライセンス数超過、バリデーションエラーの場合
     */
    public User register(RegisterRequest request) {
        log.info("User registration request: email={}, invitationToken={}", request.getEmail(), request.getInvitationToken());

        // 招待トークンを検証
        UserInvitation invitation = invitationRepository
                .findByTokenAndDeletedAtIsNullAndStatusPending(request.getInvitationToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "招待トークンが見つかりません"));

        // 有効性チェック
        if (!invitation.isValid()) {
            if (invitation.getExpiresAt().isBefore(java.time.OffsetDateTime.now())) {
                invitation.setStatus("expired");
                invitationRepository.save(invitation);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "招待トークンが無効または期限切れです");
        }

        // メールアドレスの一致チェック
        if (!invitation.getEmail().equals(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "招待トークンのメールアドレスと一致しません");
        }

        // ライセンス数チェック
        Company company = invitation.getCompany();
        if (company.getMaxUsers() != null) {
            long currentUserCount = companyMembershipRepository.countByCompanyIdAndDeletedAtIsNull(company.getId());
            if (currentUserCount >= company.getMaxUsers()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "ライセンス数の上限に達しています。現在のユーザー数: " + currentUserCount + ", 上限: " + company.getMaxUsers());
            }
        }

        // システムボットのIDを取得
        UUID systemBotId = getOrCreateSystemBot();

        // 既存ユーザーかどうかを確認
        Optional<User> existingUserOpt = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail());

        User user;
        if (existingUserOpt.isPresent()) {
            // 既存ユーザーの場合：追加登録
            user = existingUserOpt.get();
            log.info("Existing user registration: userId={}, email={}", user.getId(), user.getEmail());

            // パスワードは不要（既に設定済み）
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "既存ユーザーの場合、パスワードは不要です");
            }
        } else {
            // 新規ユーザーの場合：ユーザー作成
            log.info("New user registration: email={}", request.getEmail());

            // パスワードと氏名のバリデーション
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新規ユーザーの場合、パスワードは必須です");
            }
            if (request.getFamilyName() == null || request.getFamilyName().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新規ユーザーの場合、姓は必須です");
            }
            if (request.getFirstName() == null || request.getFirstName().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新規ユーザーの場合、名は必須です");
            }

            // パスワードをBCryptでハッシュ化
            String hashedPassword = passwordEncoder.encode(request.getPassword());

            // ユーザーエンティティを作成
            user = User.builder()
                    .email(request.getEmail())
                    .passwordHash(hashedPassword)
                    .familyName(request.getFamilyName())
                    .firstName(request.getFirstName())
                    .familyNameKana(request.getFamilyNameKana())
                    .firstNameKana(request.getFirstNameKana())
                    .status("active")
                    .createdBy(systemBotId)
                    .updatedBy(systemBotId)
                    .build();

            @SuppressWarnings("null")
            User savedUser = userRepository.save(user);
            user = savedUser;
            log.info("New user created: userId={}, email={}", user.getId(), user.getEmail());
        }

        // ラムダ式内で使用するためにfinalな変数にコピー
        final User finalUser = user;

        // 会社メンバーシップを作成（既に存在する場合はスキップ）
        CompanyMembership membership = companyMembershipRepository
                .findByUserIdAndCompanyIdAndDeletedAtIsNull(finalUser.getId(), company.getId())
                .orElseGet(() -> {
                    CompanyMembership newMembership = CompanyMembership.builder()
                            .companyId(company.getId())
                            .userId(finalUser.getId())
                            .role(invitation.getRole())
                            .joinedAt(java.time.OffsetDateTime.now())
                            .createdBy(systemBotId)
                            .updatedBy(systemBotId)
                            .build();
                    @SuppressWarnings("null")
                    CompanyMembership savedMembership = companyMembershipRepository.save(newMembership);
                    return savedMembership;
                });

        // 従業員レコードを作成（既に存在する場合はスキップ）
        Employee employee = employeeRepository
                .findByCompanyIdAndEmployeeNoAndDeletedAtIsNull(company.getId(), invitation.getEmployeeNo())
                .orElseGet(() -> {
                    // 雇用区分と入社日のデフォルト値
                    String employmentType = invitation.getEmploymentType() != null
                            ? invitation.getEmploymentType()
                            : "fulltime";
                    java.time.LocalDate hireDate = invitation.getHireDate() != null
                            ? invitation.getHireDate()
                            : java.time.LocalDate.now();

                    Employee newEmployee = Employee.builder()
                            .company(company)
                            .user(finalUser)
                            .employeeNo(invitation.getEmployeeNo())
                            .employmentType(employmentType)
                            .hireDate(hireDate)
                            .createdBy(systemBotId)
                            .updatedBy(systemBotId)
                            .build();
                    @SuppressWarnings("null")
                    Employee savedEmployee = employeeRepository.save(newEmployee);
                    return savedEmployee;
                });

        // 招待トークンを使用済みにマーク
        invitationService.markAsUsed(invitation, user);

        log.info("User registered successfully: userId={}, email={}, companyId={}, employeeId={}, membershipId={}",
                user.getId(), user.getEmail(), company.getId(), employee.getId(), membership.getId());
        return user;
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
    // private Company getOrCreateDefaultCompany(UUID systemBotId) {
    //     // デフォルトの会社コードで検索
    //     return companyRepository.findByCodeAndDeletedAtIsNull("DEFAULT")
    //             .orElseGet(() -> {
    //                 // デフォルトの会社が見つからない場合は作成
    //                 log.info("Creating default company: code=DEFAULT");

    //                 Company defaultCompany = Company.builder()
    //                         .name("デフォルト会社")
    //                         .code("DEFAULT")
    //                         .timezone("Asia/Tokyo")
    //                         .currency("JPY")
    //                         .createdBy(systemBotId)
    //                         .updatedBy(systemBotId)
    //                         .build();

    //                 @SuppressWarnings("null")
    //                 Company savedCompany = companyRepository.save(defaultCompany);

    //                 log.info("Default company created: companyId={}, code={}", savedCompany.getId(), savedCompany.getCode());
    //                 return savedCompany;
    //             });
    // }
}
