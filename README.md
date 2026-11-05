# NightDim — 수면용 화면 디머

빠른 설정(와이파이/블루투스 있는 곳) 타일 하나로 화면을 최소 밝기보다 더 어둡게 만듭니다.

**스크린샷에 어둡게 안 찍힙니다.** Screen Dimmer류 앱은 화면 위에 검은 반투명 막(오버레이)을 띄워서 캡처에도 막이 같이 찍히지만,
이 앱은 안드로이드 내장 *Extra dim(밝기 더 낮추기)* 을 제어합니다. 이건 디스플레이 출력 단계에서 적용되므로 캡처 원본엔 영향이 없습니다.

## 요구사항
- Android 12 이상

## 설치 & 최초 1회 설정
1. APK 받기: GitHub Actions → 최신 *Build APK* 실행 → `NightDim-apk` 다운로드 (또는 `./gradlew assembleRelease`)
2. 폰에 설치
3. PC에 [platform-tools(adb)](https://developer.android.com/tools/releases/platform-tools) 설치, 폰에서 개발자 옵션 → USB 디버깅 켜고 연결 후:
   ```
   adb shell pm grant com.sscrribble.nightdim android.permission.WRITE_SECURE_SETTINGS
   ```
   (Android 11+ 무선 디버깅으로도 가능. 앱을 지우기 전까지 유지)
4. 앱에서 **빠른 설정에 타일 추가** → 이제 상단바 내려서 🌙 **수면 디밍** 탭으로 켜고 끄기

## 기능
- 타일 탭: 켜기/끄기, 타일 길게 누르기: 설정 화면
- 강도 슬라이더 (0–100)
- 흔들어서 끄기: 켜져 있는 동안 폰을 빠르게 3번 흔들면 꺼짐 (상태 알림의 "끄기" 버튼으로도 가능)
- 자동 끄기: 지정한 시각(기본 07:00)이 되면 꺼짐. '알람 및 리마인더' 허용 시 정확한 시각, 아니면 몇 분 늦을 수 있음
- 옵션: 켤 때 시스템 밝기도 최소로 내리고, 끌 때 원래 밝기/자동밝기 복원 (시스템 설정 수정 권한 필요)
