package kr.co.lovelydream.global.enums

enum class ResponseCode(val code: String, val message: String) {
    SUCCESS("SUCCESS", "정상 처리되었습니다."),
    FAIL("FAIL", "서버 처리 실패"),
    VALIDATION_ERROR("VALIDATION_ERROR", "입력값이 잘못되었습니다."),
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다."),

    // 인증 관련 반환 코드
    AUTH_CODE_EXPIRED("A001", "인증 코드가 만료되었습니다."),
    AUTH_CODE_MISMATCH("A002", "인증 코드가 일치하지 않습니다."),
    AUTH_INVALID_CREDENTIAL("A003", "이메일 또는 비밀번호가 일치하지 않습니다."),
    AUTH_EMAIL_ALREADY_EXISTS("A004", "이미 존재하는 이메일입니다."),
    AUTH_UNAUTHORIZED("A005", "인증되지 않은 사용자입니다."),
    AUTH_EMAIL_SEND_FAILED("A006", "이메일 전송에 실패했습니다."),

    // 약관 관련 반환 코드
    TERMS_NOT_FOUND("T001", "약관의 최신 버전을 찾을 수 없습니다."),

    // 회원 관련 반환 코드
    MEMBER_NOT_FOUND("M001", "존재하지 않는 회원입니다."),
}