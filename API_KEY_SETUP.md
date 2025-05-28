# 🔑 카카오 지도 API 키 설정 가이드

## 🚨 현재 상황
- **현재 API 키**: `145267004b17a2bfe6fadd60b7044b66`
- **에러**: MapAuthException(401): Unauthorized
- **원인**: 카카오 개발자 콘솔에 앱 정보 미등록

## 📍 키 설정 위치

### 현재 소스에서 키 설정:
```kotlin
// app/src/main/java/com/dev_oms/trailmate/TrailMateApplication.kt
class TrailMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // ⬇️ 여기서 카카오맵 API 키 설정
        KakaoMapSdk.init(this, "145267004b17a2bfe6fadd60b7044b66")
    }
}
```

## 🛠️ 해결 방법

### Option 1: 기존 API 키 활성화 (권장)
1. **카카오 개발자 콘솔 접속**
   - https://developers.kakao.com
   - 기존 앱 찾기 (API 키: `145267004b17a2bfe6fadd60b7044b66`)

2. **Android 플랫폼 설정**
   ```
   앱 설정 > 플랫폼 > Android 플랫폼 추가/수정
   
   패키지명: com.dev_oms.trailmate
   키 해시: z7mKdyTfmLmyq5vUFAsDMHcnZBo=
   ```

### Option 2: 새 API 키 발급
1. **새 앱 생성**
   - 카카오 개발자 콘솔에서 새 애플리케이션 추가
   
2. **Android 플랫폼 등록**
   ```
   패키지명: com.dev_oms.trailmate
   키 해시: z7mKdyTfmLmyq5vUFAsDMHcnZBo=
   ```

3. **새 API 키로 교체**
   ```kotlin
   // TrailMateApplication.kt 수정
   KakaoMapSdk.init(this, "새로운_네이티브_앱_키")
   ```

## 🔍 키 해시 재확인 방법

터미널에서 실행:
```bash
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android -keypass android | openssl sha1 -binary | openssl base64
```

결과: `z7mKdyTfmLmyq5vUFAsDMHcnZBo=`

## ✅ 정상 작동 확인

API 키 등록 후 앱 재시작하면:
- 401 에러 사라짐
- 지도 정상 표시
- 로그에서 "✅ Map initialized successfully" 확인

## 🚀 빠른 해결

**99% 확률**: 카카오 개발자 콘솔에서 키 해시만 등록하면 즉시 해결됩니다! 