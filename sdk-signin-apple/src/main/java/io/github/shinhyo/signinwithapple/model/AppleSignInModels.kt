package io.github.shinhyo.signinwithapple.model

/**
 * 애플 로그인 결과로 받는 사용자 자격 증명
 * idToken만 포함하는 단순화된 버전
 */
data class AppleIdCredential(
    val identityToken: String?
)
