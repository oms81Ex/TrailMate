#!/bin/bash

echo "=== 카카오맵 API 인증 테스트 ==="
echo ""

# API 키와 정보
APP_KEY="bd9941dd7fbc5a5a94c7ff1da148ef4d"
PACKAGE_NAME="com.dev_oms.trailmate"
KEY_HASH="z7mKdyTfmLmyq5vUFAsDMHcnZBo="

echo "테스트 정보:"
echo "- API 키: $APP_KEY"
echo "- 패키지명: $PACKAGE_NAME"
echo "- 키 해시: $KEY_HASH"
echo ""

# 카카오맵 인증 API 호출
echo "카카오맵 인증 API 호출 중..."
echo ""

curl -v -X GET "https://dapi.kakao.com/v2/maps/vector/auth" \
  -H "Accept: application/json" \
  -H "Authorization: KakaoAK $APP_KEY" \
  -H "KA: mapSdk/2.12.8 os/android-36 lang/en-US origin/$KEY_HASH device/sdk_gphone64_arm64 android_pkg/$PACKAGE_NAME"

echo ""
echo ""
echo "=== 일반 카카오 API 테스트 (앱 정보 확인) ==="
echo ""

# 일반적인 카카오 API 테스트
curl -v -X GET "https://kapi.kakao.com/v1/api/talk/profile" \
  -H "Authorization: KakaoAK $APP_KEY"
