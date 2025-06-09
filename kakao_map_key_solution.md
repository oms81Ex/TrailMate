# 카카오맵 API 키 문제 해결

## 문제
- 현재 사용 중인 키: `bd9941dd7fbc5a5a94c7ff1da148ef4d`
- 이 키는 **Admin 키**입니다
- 카카오맵은 **네이티브 앱 키**를 사용해야 합니다

## 해결 방법

1. [카카오 개발자 콘솔](https://developers.kakao.com) 로그인
2. TrailMate 앱 선택
3. **[내 애플리케이션] > [앱 키]** 메뉴로 이동
4. **네이티브 앱 키** 복사 (Admin 키가 아닌)
5. AndroidManifest.xml 수정:

```xml
<meta-data
    android:name="com.kakao.vectormap.APP_KEY"
    android:value="여기에_네이티브_앱_키_입력" />
```

## 키 종류 설명
- **네이티브 앱 키**: Android/iOS 앱에서 사용
- **JavaScript 키**: 웹에서 사용
- **REST API 키**: 서버에서 API 호출 시 사용
- **Admin 키**: 관리자 권한이 필요한 API에서 사용

카카오맵 SDK는 **네이티브 앱 키**만 지원합니다!
