# WelStory DID Android Client

간단한 웹뷰 기반 DID 클라이언트

## 빌드 방법

### Android Studio 사용

1. Android Studio 설치
2. 프로젝트 열기: `File > Open > AndroidDIDClient 폴더 선택`
3. Gradle 동기화 대기
4. 빌드: `Build > Build Bundle(s) / APK(s) > Build APK(s)`
5. APK 위치: `app/build/outputs/apk/release/app-release.apk`

### 명령줄 빌드

```bash
cd AndroidDIDClient
./gradlew assembleRelease
```

## 설정 변경

`MainActivity.kt` 파일에서 설정 변경:

```kotlin
private val serverUrl = "http://210.219.229.46:8081"
private val restaurantCode = "RST001"
private val displayId = "1"
private val refreshInterval = 60000L // 1분
```

## 기능

- 전체 화면 표시
- 1분마다 자동 새로고침
- HTTP 연결 지원 (안드로이드 9+)
- 가로 모드 고정

## 설치

```bash
adb install app-release.apk
```
