#!/bin/bash

# 이 스크립트를 실행하기 전에 NATIVE_APP_KEY를 실제 네이티브 앱 키로 변경하세요
NATIVE_APP_KEY="14526700db17a2bfe6fadd60b70d4b66"

echo "=== 카카오맵 API 인증 테스트 (네이티브 앱 키) ==="
echo ""

# API 키와 정보
PACKAGE_NAME="com.dev_oms.trailmate"
KEY_HASH="z7mKdyTfmLmyq5vUFAsDMHcnZBo="

echo "테스트 정보:"
echo "- 네이티브 앱 키: $NATIVE_APP_KEY"
echo "- 패키지명: $PACKAGE_NAME"
echo "- 키 해시: $KEY_HASH"
echo ""

# 카카오맵 인증 API 호출
echo "카카오맵 인증 API 호출 중..."
echo ""

curl -v -X GET "https://dapi.kakao.com/v2/maps/vector/auth" \
  -H "Accept: application/json" \
  -H "Authorization: KakaoAK $NATIVE_APP_KEY" \
  -H "KA: mapSdk/2.12.8 os/android-36 lang/en-US origin/$KEY_HASH device/sdk_gphone64_arm64 android_pkg/$PACKAGE_NAME"

echo ""
echo ""
echo "응답이 200 OK면 성공입니다!"
