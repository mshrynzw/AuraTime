package com.auratime.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * システム定数クラス
 *
 * <p>
 * アプリケーション全体で使用する定数を定義するクラスです。
 * 環境変数や設定ファイルから値を読み込みます。
 * </p>
 *
 * <h3>定数の種類</h3>
 * <ul>
 * <li>システムボット関連の定数</li>
 * </ul>
 *
 * <h3>使用方法</h3>
 * <p>
 * このクラスはSpringのコンポーネントとして管理されているため、
 * 他のコンポーネントに注入して使用するか、staticメソッドでアクセスできます。
 * </p>
 */
@Component
public class SystemConstants {

    /**
     * システムボットのメールアドレス（staticフィールド）
     *
     * <p>
     * システムが自動的に作成・更新するデータの作成者として使用される
     * システムボットユーザーのメールアドレスです。
     * </p>
     */
    public static String SYSTEM_BOT_EMAIL;

    /**
     * システムボットのパスワード（staticフィールド）
     *
     * <p>
     * システムボットユーザーのパスワードです。
     * 実際にはログインに使用されませんが、ユーザーエンティティ作成時に必要です。
     * </p>
     */
    public static String SYSTEM_BOT_PASSWORD;

    /**
     * システムボットのメールアドレス（インスタンスフィールド、環境変数から読み込む）
     */
    @Value("${system.bot.email}")
    private String systemBotEmail;

    /**
     * システムボットのパスワード（インスタンスフィールド、環境変数から読み込む）
     */
    @Value("${system.bot.password}")
    private String systemBotPassword;

    /**
     * 初期化処理
     * 環境変数から読み込んだ値をstaticフィールドに設定
     */
    @PostConstruct
    private void init() {
        SYSTEM_BOT_EMAIL = systemBotEmail;
        SYSTEM_BOT_PASSWORD = systemBotPassword;
    }
}

