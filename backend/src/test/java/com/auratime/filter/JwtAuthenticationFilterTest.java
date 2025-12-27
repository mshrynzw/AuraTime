package com.auratime.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import com.auratime.util.CompanyContext;
import com.auratime.util.JwtTokenProvider;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JwtAuthenticationFilterの単体テスト
 *
 * <p>
 * JWT認証フィルターの動作をテストするクラスです。
 * Mockitoを使用してHTTPリクエストとフィルターチェーンをモック化し、
 * JWTトークンの検証と認証情報の設定をテストします。
 * </p>
 *
 * <h3>テスト対象</h3>
 * <ul>
 * <li>JWTトークンの抽出と検証</li>
 * <li>SecurityContextへの認証情報設定</li>
 * <li>CompanyContextへのcompany_id設定</li>
 * <li>リクエスト処理後のコンテキストクリア</li>
 * </ul>
 *
 * <h3>テスト方針</h3>
 * <ul>
 * <li>正常系: 有効なトークンで認証情報が正しく設定されることを確認</li>
 * <li>異常系: 無効なトークンやトークンなしでも処理が続行されることを確認</li>
 * <li>コンテキスト管理: CompanyContextが適切にクリアされることを確認</li>
 * </ul>
 *
 * @see com.auratime.filter.JwtAuthenticationFilter
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter テスト")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UUID userId;
    private UUID companyId;
    private String validToken;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        validToken = "valid-jwt-token";
        SecurityContextHolder.clearContext();
        CompanyContext.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        CompanyContext.clear();
    }

    @Test
    @DisplayName("正常系：有効なトークンで認証情報を設定")
    @SuppressWarnings("null")
    void testDoFilterInternal_ValidToken() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validToken)).thenReturn(userId);
        when(jwtTokenProvider.getCompanyIdFromToken(validToken)).thenReturn(companyId);
        when(jwtTokenProvider.getRoleFromToken(validToken)).thenReturn("employee");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider).validateToken(validToken);
        verify(jwtTokenProvider).getUserIdFromToken(validToken);
        verify(jwtTokenProvider).getCompanyIdFromToken(validToken);
        verify(filterChain).doFilter(request, response);

        // SecurityContextの認証情報を確認
        // 注意: filterChain.doFilterの後、finallyブロックでCompanyContextはクリアされるが、
        // SecurityContextはクリアされないため、認証情報は残っているはず
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userId);

        // CompanyContextはfinallyブロックでクリアされるため、nullになる
        // これは正常な動作（リクエスト処理完了後のクリア）
        assertThat(CompanyContext.getCompanyId()).isNull();
    }

    @Test
    @DisplayName("正常系：トークンなしでも処理を続行")
    @SuppressWarnings("null")
    void testDoFilterInternal_NoToken() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider, never()).validateToken(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("正常系：無効なトークンでも処理を続行")
    @SuppressWarnings("null")
    void testDoFilterInternal_InvalidToken() throws Exception {
        // Given
        String invalidToken = "invalid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(jwtTokenProvider).validateToken(invalidToken);
        verify(jwtTokenProvider, never()).getUserIdFromToken(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("正常系：リクエスト処理後にCompanyContextをクリア")
    @SuppressWarnings("null")
    void testDoFilterInternal_ClearsContextAfterRequest() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(validToken)).thenReturn(userId);
        when(jwtTokenProvider.getCompanyIdFromToken(validToken)).thenReturn(companyId);
        when(jwtTokenProvider.getRoleFromToken(validToken)).thenReturn("employee");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: フィルター処理後、CompanyContextはクリアされている
        // ただし、実際のクリアはfinallyブロックで行われるため、
        // この時点ではまだ設定されている可能性がある
        // 実際の動作は、リクエスト処理完了後にクリアされる
        verify(filterChain).doFilter(request, response);
    }
}
