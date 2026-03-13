# Android DID Client 빌드 가이드

## 새로운 기능
- **API 통신**: CMS API와 실시간 통신으로 스케줄 조회
- **동적 템플릿 전환**: 스케줄에 따른 자동 템플릿 변경
- **화면 제어**: 스케줄 없을 때 자동 화면 OFF
- **설정 화면**: 서버 정보 및 새로고침 간격 설정
- **리모컨 지원**: TV 리모컨으로 설정 접근

## API 통신 기능
- **스케줄 조회**: `/api/v1/schedule/SelectSchedule`
- **스케줄 업데이트**: `/api/v1/schedule/UpdateSchedule`
- **실시간 반영**: 설정된 간격으로 자동 스케줄 확인
- **오류 처리**: 네트워크 오류 시 재시도 및 로그

## 설정 접근 방법
1. **메뉴 키**: TV 리모컨의 메뉴 키 누르기
2. **설정 키**: TV 리모컨의 설정 키 누르기  
3. **뒤로가기 길게 누르기**: 뒤로가기 키를 길게 누르기

## 설정 항목
- **서버 URL**: CMS API 서버 주소 (예: http://210.219.229.46:8081)
- **레스토랑 코드**: 레스토랑 식별 코드 (예: RST001)
- **디스플레이 ID**: 디스플레이 장치 ID (예: 1)
- **새로고침 간격**: API 호출 간격 (초 단위, 최소 10초)

## 동작 방식
1. **앱 시작**: 설정값으로 API 클라이언트 초기화
2. **스케줄 조회**: API 호출하여 현재 스케줄 확인
3. **템플릿 로드**: 스케줄에 맞는 템플릿 URL 로드
4. **자동 갱신**: 설정된 간격으로 스케줄 재확인
5. **화면 제어**: 스케줄 없으면 검은 화면 표시

## 빌드 방법
1. Android Studio에서 프로젝트 열기
2. **File** → **Sync Project with Gradle Files**
3. **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
4. 생성된 APK: `app/build/outputs/apk/debug/app-debug.apk`

## 설치 방법
```bash
adb install app-debug.apk
```

## 로그 확인
```bash
adb logcat | grep -E "(MainActivity|ApiClient)"
```

## 기본 설정값
- 서버 URL: http://210.219.229.46:8081
- 레스토랑 코드: RST001
- 디스플레이 ID: 1
- 새로고침 간격: 60초

## API 응답 처리
- **actionType "00"**: 변경 없음 (현재 상태 유지)
- **actionType "11"**: 새 템플릿 로드
- **actionType "99"**: 화면 OFF (검은 화면)