# 카카오맵 API 키 체크리스트

## 1. 현재 설정된 정보
- 패키지명: com.dev_oms.trailmate
- 키 해시: z7mKdyTfmLmyq5vUFAsDMHcnZBo=
- API 키: bd9941dd7fbc5a5a94c7ff1da148ef4d

## 2. 확인 사항
- [ ] 카카오 개발자 콘솔에서 카카오맵 API 사용 설정이 ON인가?
- [ ] AndroidManifest.xml에 올바른 API 키가 설정되어 있는가?
- [ ] API 키가 "네이티브 앱 키"인가? (REST API 키가 아닌)

## 3. AndroidManifest.xml 확인
```xml
<meta-data
    android:name="com.kakao.vectormap.APP_KEY"
    android:value="bd9941dd7fbc5a5a94c7ff1da148ef4d"/>
```
