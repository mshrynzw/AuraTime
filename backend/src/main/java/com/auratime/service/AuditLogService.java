package com.auratime.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auratime.domain.AuditLog;
import com.auratime.repository.AuditLogRepository;
import com.auratime.util.CompanyContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 監査ログサービス
 *
 * <p>
 * 業務データの変更履歴を記録するサービスクラスです。
 * Spring AOPから呼び出され、データの作成・更新・削除時に自動的に監査ログを記録します。
 * </p>
 *
 * <h3>主な機能</h3>
 * <ul>
 * <li>監査ログの記録（誰が、いつ、何を、どのような操作を）</li>
 * <li>変更前後のデータをJSON形式で保存</li>
 * <li>リクエストIDによる操作の追跡</li>
 * </ul>
 *
 * <h3>エラーハンドリング</h3>
 * <p>
 * 監査ログの記録に失敗しても、メイン処理は続行されます。
 * これにより、監査ログの記録エラーが業務処理に影響を与えることを防ぎます。
 * </p>
 *
 * <h3>マルチテナント対応</h3>
 * <p>
 * CompanyContextから会社IDを取得し、監査ログに記録します。
 * これにより、各会社の監査ログが分離されます。
 * </p>
 *
 * @see com.auratime.domain.AuditLog
 * @see com.auratime.util.CompanyContext
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditLogService {

    /** 監査ログリポジトリ */
    private final AuditLogRepository auditLogRepository;

    /** JSONマッパー（変更前後のデータをJSON形式に変換） */
    private final ObjectMapper objectMapper;

    /**
     * 監査ログを記録
     *
     * <p>
     * データの変更履歴を監査ログとして記録します。
     * 変更前後のデータをJSON形式に変換して保存します。
     * </p>
     *
     * <h3>記録される情報</h3>
     * <ul>
     * <li><strong>company_id</strong>: CompanyContextから取得（マルチテナント分離）</li>
     * <li><strong>actor_user_id</strong>: 現在の認証ユーザーID</li>
     * <li><strong>action</strong>: 操作種別（"create"|"update"|"delete"）</li>
     * <li><strong>target_type</strong>: 対象テーブル名（例: "m_users"）</li>
     * <li><strong>target_id</strong>: 対象エンティティのID</li>
     * <li><strong>before_data</strong>: 変更前のデータ（JSON形式、update/delete時のみ）</li>
     * <li><strong>after_data</strong>: 変更後のデータ（JSON形式、create/update時のみ）</li>
     * <li><strong>request_id</strong>: HTTPリクエストのX-Request-Idヘッダーの値</li>
     * <li><strong>happened_at</strong>: 変更が発生した日時</li>
     * </ul>
     *
     * <h3>エラーハンドリング</h3>
     * <p>
     * 監査ログの記録に失敗しても、メイン処理は続行されます。
     * これにより、監査ログの記録エラーが業務処理に影響を与えることを防ぎます。
     * エラーはログに記録されます。
     * </p>
     *
     * <h3>JSON変換</h3>
     * <p>
     * 変更前後のデータは、ObjectMapperを使用してJSON形式に変換されます。
     * nullの場合は、そのままnullとして保存されます。
     * </p>
     *
     * @param action     操作種別（"create"|"update"|"delete"）
     * @param targetType 対象テーブル名（例: "m_users"）
     * @param targetId   対象エンティティのID
     * @param beforeData 変更前のデータ（null可）
     * @param afterData  変更後のデータ（null可）
     * @param requestId  HTTPリクエストのX-Request-Idヘッダーの値（null可）
     */
    @SuppressWarnings("null")
    public void record(String action, String targetType, UUID targetId, Object beforeData, Object afterData,
            String requestId) {
        try {
            // CompanyContextから会社IDを取得（マルチテナント分離）
            UUID companyId = CompanyContext.getCompanyId();

            // 現在の認証ユーザーIDを取得
            UUID actorUserId = getCurrentUserId();

            // 変更前後のデータをJSON形式に変換
            // nullの場合は、そのままnullとして保存
            String beforeDataJson = beforeData != null ? objectMapper.writeValueAsString(beforeData) : null;
            String afterDataJson = afterData != null ? objectMapper.writeValueAsString(afterData) : null;

            // 監査ログエンティティを作成
            AuditLog auditLog = AuditLog.builder()
                    .companyId(companyId)
                    .actorUserId(actorUserId)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .beforeData(beforeDataJson)
                    .afterData(afterDataJson)
                    .requestId(requestId)
                    .happenedAt(OffsetDateTime.now())
                    .build();

            // 監査ログを保存
            auditLogRepository.save(auditLog);
            log.debug("Audit log recorded: action={}, targetType={}, targetId={}, companyId={}, actorUserId={}",
                    action, targetType, targetId, companyId, actorUserId);
        } catch (Exception e) {
            // 監査ログの記録に失敗しても、メイン処理は続行
            // エラーはログに記録するが、例外は再スローしない
            log.error("Failed to record audit log: action={}, targetType={}, targetId={}", action, targetType, targetId,
                    e);
        }
    }

    /**
     * 現在のユーザーIDを取得
     *
     * <p>
     * Spring SecurityのSecurityContextから認証情報を取得し、
     * 認証済みユーザーのIDを返却します。
     * </p>
     *
     * <h3>処理フロー</h3>
     * <ol>
     * <li>SecurityContextからAuthenticationオブジェクトを取得</li>
     * <li>認証済みかつPrincipalがUUID型の場合、そのUUIDを返却</li>
     * <li>それ以外の場合はnullを返却（システムボットや初期データ投入時など）</li>
     * </ol>
     *
     * <h3>注意事項</h3>
     * <ul>
     * <li>認証されていない状態（初期データ投入時など）ではnullを返却</li>
     * <li>将来的には、システムボットのIDを返すように実装を変更する予定</li>
     * </ul>
     *
     * @return 現在のユーザーID（認証されていない場合はnull）
     * @see com.auratime.filter.JwtAuthenticationFilter
     */
    private UUID getCurrentUserId() {
        // Spring SecurityのSecurityContextから認証情報を取得
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 認証済みで、PrincipalがUUID型の場合、そのUUIDを返却
        // JwtAuthenticationFilterで設定されたPrincipalはユーザーID（UUID）です
        if (authentication != null && authentication.getPrincipal() instanceof UUID) {
            return (UUID) authentication.getPrincipal();
        }

        // 認証されていない場合（初期データ投入時、システムボット実行時など）はnullを返却
        // TODO: 将来的には、システムボットのIDを返すように実装を変更
        return null;
    }
}
