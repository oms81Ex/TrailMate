# 🗺️ 카카오 지도 하얀 화면 문제 해결 가이드

## 🚨 문제 현상
TrailMate 앱에서 지도 영역이 하얀색으로만 표시되고 실제 지도가 나오지 않는 문제

## 🔍 원인 분석
카카오 지도 API에서 하얀 화면이 나오는 주요 원인들:

### 1. **키 해시(Key Hash) 등록 문제** ⭐ **가장 일반적인 원인**
- 카카오 개발자 콘솔에 현재 앱의 키 해시가 등록되지 않음
- 디버그 키와 릴리즈 키의 해시값이 다름

### 2. **API 키 인증 실패**
- 잘못된 네이티브 앱 키 사용
- API 키 권한 설정 오류

### 3. **네트워크 연결 문제**
- 인터넷 연결 불안정
- 방화벽/프록시 차단

## ✅ 해결 방법

### 1단계: 현재 키 해시 확인
```bash
# macOS/Linux에서 디버그 키 해시 생성
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android -keypass android | openssl sha1 -binary | openssl base64
```

**현재 TrailMate 디버그 키 해시:**
```
z7mKdyTfmLmyq5vUFAsDMHcnZBo=
```

### 2단계: 카카오 개발자 콘솔에서 키 해시 등록

1. [카카오 개발자 콘솔](https://developers.kakao.com) 접속
2. **내 애플리케이션** > **앱 설정** > **플랫폼** 메뉴
3. **Android 플랫폼 설정**에서 **키 해시** 입력:
   ```
   z7mKdyTfmLmyq5vUFAsDMHcnZBo=
   ```
4. **저장** 버튼 클릭

### 3단계: API 키 확인
TrailMateApplication.kt에서 사용 중인 API 키:
```kotlin
KakaoMapSdk.init(this, "145267004b17a2bfe6fadd60b7044b66")
```

### 4단계: 네트워크 설정 확인
network_security_config.xml에서 카카오 도메인 허용 여부:
```xml
<domain-config>
    <domain includeSubdomains="true">devrepo.kakao.com</domain>
    <domain includeSubdomains="true">map-api.kakaomobility.com</domain>
    <domain includeSubdomains="true">tile.map.kakao.com</domain>
</domain-config>
```

## 🛠️ 앱 내 디버깅 기능

TrailMate 앱에는 지도 문제 디버깅을 위한 기능들이 내장되어 있습니다:

### 1. 로딩 상태 표시
- 지도 로딩 중: "지도 로딩 중..." 메시지와 함께 키 해시 정보 표시
- 30초 타임아웃: 로딩이 30초 이상 걸리면 자동으로 오류 표시

### 2. 상세 에러 정보
- 키 해시 표시: `z7mKdyTfmLmyq5vUFAsDMHcnZBo=`
- 해결 방법 안내
- 재시도 버튼 제공

### 3. 로그 출력
```bash
# 로그 확인 (ADB 필요)
adb logcat | grep -E "KakaoMapView|HomeScreen"
```

주요 로그 메시지:
- `✅ Map initialized successfully`: 지도 초기화 성공
- `MAP ERROR DETECTED`: 지도 로딩 오류
- `API KEY 문제로 인한 하얀 화면 가능성 높음`: 인증 오류

## 🎯 권장 해결 순서

1. **즉시 해결**: 키 해시 등록 (`z7mKdyTfmLmyq5vUFAsDMHcnZBo=`)
2. **네트워크 확인**: Wi-Fi/모바일 데이터 연결 상태
3. **앱 재시작**: 설정 변경 후 앱 완전 종료 후 재시작
4. **릴리즈 빌드**: 릴리즈 버전에서는 별도 키 해시 필요

## 🚀 릴리즈 배포 시 주의사항

릴리즈 빌드에서는 다른 키 해시가 필요합니다:

```bash
# 릴리즈 키스토어의 키 해시 생성
keytool -exportcert -alias [ALIAS_NAME] -keystore [KEYSTORE_PATH] | openssl sha1 -binary | openssl base64
```

Google Play 앱 번들 사용 시:
1. Google Play Console에서 **앱 서명** 메뉴 확인
2. **SHA-1 인증서 지문** 복사
3. 온라인 도구로 Base64 변환하여 키 해시 생성

## 📞 추가 지원

문제가 지속될 경우:
1. GitHub Issues에 로그와 함께 제보
2. 카카오 개발자 포럼 문의
3. 대체 지도 SDK 고려 (네이버 지도, Google Maps)

---

**⚡ 빠른 해결**: 대부분의 경우 키 해시 등록(`z7mKdyTfmLmyq5vUFAsDMHcnZBo=`)만으로 해결됩니다! 