# 점메추리알 🥚

카카오톡 일반 단체방의 공식 메시지 읽기/보내기 API는 제공되지 않으므로, 이 MVP는 **모바일 웹 참여 링크 + 카카오톡 공유**를 사용합니다. 단체방에 초대 링크를 붙여 넣고, 참여자는 웹에서 메뉴를 등록합니다. 앱은 단체방 메시지나 연락처를 수집하지 않습니다.

## 포함 기능

- 방 생성 시 회사 주소와 점심 시작·종료 시간 등록, 방장 기능 토큰 발급
- 오전 10시 44분까지 익명 메뉴 등록·변경·취소, 10시 45분 자동 추첨
- 전체 참여 목록과 중복 메뉴 제거
- 중복 제거된 후보마다 동일한 확률을 주는 추첨과 #룰렛돌리기 결과 애니메이션
- 결과 확인, 모집 마감
- 무작위 참여 토큰, DB에는 참여 토큰 SHA-256 해시만 저장, 방장 HMAC 토큰
- 참여자별 DB 유니크 제약, 방 상태 버전 기반 추첨 동시성 제어
- 모바일 귀여운 메추리알 웹 화면, 카카오톡 공유 시트(미지원 환경은 링크 복사)
- 룰렛 결과 공개 후 회사 주소 반경 1km 식당 자동 추천. 최근 발견 → 메뉴 언급 수 → 직선거리 → 서비스 내 평가 순으로 정렬

‘최근 발견’은 서비스에 처음 등록된 시점이며 실제 개업일이 아닙니다. 장소 검색은 설정된 OSM 또는 카카오 제공자를 사용합니다. 무료 OSM 공용 서버는 이용량 제한이 있으므로 소규모 시연 용도로 사용하고, 사용량이 늘면 별도 장소 데이터 제공자를 설정해야 합니다.

## 프로젝트

```text
bot-core        도메인 모델
bot-service     유스케이스와 추첨 규칙
bot-persistence PostgreSQL/JPA/Flyway 어댑터
bot-api         REST API
bot-app         Spring Boot 실행 앱, 정적 모바일 웹
```

## 실행

필요: JDK 21, Docker Compose. Spring Boot 3.5.16을 사용합니다. 이 버전은 Java 21을 지원하고 Gradle 8.4 이상이 필요합니다. [공식 요구사항](https://docs.spring.io/spring-boot/3.5/system-requirements.html).

```powershell
Copy-Item .env.example .env
$env:DATABASE_PASSWORD='replace-with-a-unique-database-password'
docker compose up -d db
$env:DATABASE_URL='jdbc:postgresql://localhost:5432/jeommechurial'
$env:DATABASE_USERNAME='jeommechurial'
$env:DATABASE_PASSWORD='replace-with-a-unique-database-password'
$env:OWNER_SECRET='replace-with-a-long-random-secret'
.\gradlew.bat test
.\gradlew.bat :bot-app:bootRun
```

브라우저에서 `http://localhost:8080/demo.html`을 열면 카카오톡 같은 대화 화면에서 방장이 회사 주소와 점심 시작·종료 시간을 등록하고, 익명 메뉴 입력과 룰렛을 시연할 수 있습니다. 이는 실제 카카오톡 연결이 아닌 가상 대화입니다. `http://localhost:8080`에서 실제 방을 만들 때 회사 주소와 점심 시작·종료 시간을 저장하고 참여 링크를 카카오톡 단체방에 공유할 수 있습니다. Supabase 사용 시 프로젝트의 PostgreSQL 연결 문자열과 DB 인증 정보를 환경 변수로 설정하세요. 배포 환경은 SSL을 포함한 Supabase 연결 문자열을 사용하고 `OWNER_SECRET`을 32자 이상의 임의 비밀값으로 설정하세요. `.env`는 저장소에 커밋하지 마세요.

## 공개 배포: Render + PostgreSQL

Docker Desktop은 내 PC에서 실행할 때 쓰며, 카카오 챗봇이 접속할 공개 HTTPS 주소를 얻으려면 서버 배포가 필요합니다. 이 저장소의 `Dockerfile`을 Render 웹 서비스에 배포하면 `https://서비스이름.onrender.com` 주소가 제공됩니다.

1. 이 프로젝트를 GitHub 저장소에 올립니다. `.env`와 API 키는 올리지 마세요. Git을 사용한다면 프로젝트 폴더에서 `git init`, `git add .`, `git commit -m "Initial deploy"` 후 GitHub가 표시하는 원격 저장소 연결·push 명령을 실행합니다.
2. [Render 대시보드](https://dashboard.render.com/)에서 **New → Postgres**를 만들어 같은 지역의 데이터베이스를 준비합니다. 운영 용도라면 장기간 유지되는 요금제를 선택합니다.
3. **New → Web Service → GitHub 저장소 연결**에서 이 저장소를 고르고 **Language/Runtime: Docker**, **Dockerfile path: `./Dockerfile`**로 설정합니다. 인스턴스를 계속 실행하는 요금제를 선택해야 정해진 점심시간에 자동 알림이 안정적으로 발송됩니다. HTTP Health Check Path는 `/actuator/health`로 설정할 수 있습니다.
4. 웹 서비스 **Environment**에 아래 값을 입력합니다. Render Postgres의 내부 연결 정보에서 호스트·포트·데이터베이스명·사용자명·비밀번호를 복사합니다. `DATABASE_URL`은 `postgresql://...` 형식 대신 **JDBC** 형식으로 입력합니다.

   ```text
   DATABASE_URL=jdbc:postgresql://DB호스트:5432/DB이름
   DATABASE_USERNAME=DB사용자
   DATABASE_PASSWORD=DB비밀번호
   OWNER_SECRET=32자이상의임의문자열
   KAKAO_SKILL_SECRET=별도의긴임의문자열
   KAKAO_REST_API_KEY=카카오디벨로퍼스REST_API키
   PUBLIC_BASE_URL=https://서비스이름.onrender.com
   KAKAO_BOT_ID=카카오챗봇ID
   KAKAO_LUNCH_EVENT_NAME=lunch_recommendation
   ```

5. 배포가 끝나면 `https://서비스이름.onrender.com/actuator/health`를 열어 `UP`을 확인합니다. 앱 시작 시 Flyway가 테이블을 생성·변경합니다.
6. 카카오 챗봇 관리자센터에서 스킬 URL을 `https://서비스이름.onrender.com/kakao/skill`로, 요청 헤더 `X-Skill-Secret`을 위의 값으로 등록하고 스킬을 블록에 연결합니다. 채널 챗봇 배포 후 실제 채널 대화에서 `회사설정 12:00 13:00 회사주소`를 입력합니다.

Render 무료 웹 서비스는 유휴 시 중지될 수 있고 무료 PostgreSQL에는 보존 기간 제한이 있어 정시 알림과 장기 데이터 보관 용도로는 적합하지 않습니다. [Render 무료 플랜 안내](https://render.com/docs/free), [Docker 배포 안내](https://render.com/docs/docker), [Postgres 연결 안내](https://render.com/docs/postgresql-creating-connecting).

`kakaoBot` 배포에는 `.github/workflows/keepalive.yml`이 10분마다 공개 `/actuator/health` 주소를 호출합니다. 서버 내부 예약 작업과 달리 외부에서 호출하므로 유휴 중지 위험을 줄이지만, GitHub 예약 작업 지연·중단이나 Render 재시작이 있으면 정시 실행을 보장하지 않습니다. 공개 저장소는 60일 동안 활동이 없으면 예약 워크플로가 자동으로 꺼질 수 있습니다. [GitHub 예약 작업 안내](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows).


## 카카오톡 챗봇 관리자센터 베타 스킬 연결

앱에는 카카오 스킬 요청용 `POST /kakao/skill` 엔드포인트가 포함됩니다. 카카오 챗봇 스킬 서버는 HTTP POST JSON으로 호출되며, 이 엔드포인트는 카카오 스킬 응답 2.0의 `template.outputs.simpleText` 형식으로 답합니다. [스킬 서버 가이드](https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill), [응답 JSON 형식](https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format).

### 공개 URL 준비

카카오 서버는 사용자 PC의 `localhost`에 접속할 수 없으므로 HTTPS가 적용된 외부 공개 주소로 앱을 배포해야 합니다. `PUBLIC_BASE_URL`에 그 주소를 넣고 `KAKAO_SKILL_SECRET`에 충분히 긴 별도 임의 비밀값을 설정합니다. 관리자센터의 스킬 설정에서 요청 헤더 `X-Skill-Secret`에 같은 값을 넣습니다. 로컬 브라우저 UI를 쓸 때는 `PUBLIC_BASE_URL`이 참여 링크 생성에도 사용됩니다.

### 베타 챗봇에서 연결

1. 카카오톡 챗봇 관리자센터에서 봇을 만들고 개발 채널을 연결합니다.
2. **스킬** 메뉴에서 스킬을 추가합니다. URL은 `https://공개도메인/kakao/skill`, 요청 헤더는 `X-Skill-Secret: 설정한 비밀값`으로 입력합니다.
3. 스킬 테스트 요청 본문에 아래와 같은 카카오 스킬 payload를 넣습니다. `userRequest.user.id`에는 임의의 테스트 사용자 키를 지정합니다.

```json
{
  "userRequest": {
    "utterance": "방 만들기 우리팀 점심",
    "user": { "id": "test-user-001", "type": "botUserKey" }
  }
}
```

4. 블록을 만들고 사용자 발화 예시(예: `회사설정`, `오늘점심`, `투표`, `좋아요`, `싫어요`, `알림켜기`, `알림끄기`, `방 만들기`, `메뉴`, `목록`, `#룰렛돌리기`, `결과`)를 등록한 다음, 해당 블록의 **스킬 응답**에 새 스킬을 연결합니다. 응답을 스킬 데이터로 사용하도록 설정하고 저장합니다.
5. 챗봇 테스트에서 `방 만들기 우리팀 점심`을 보내고 응답의 방 ID와 참여/방장 토큰을 확인합니다. 같은 채널 챗봇 대화에서 다음 명령을 입력합니다.

```text
메뉴 <방ID> <참여토큰> 김치찌개
목록 <방ID> <참여토큰>
#룰렛돌리기 <방ID> <참여토큰>

결과 <방ID> <참여토큰>
취소 <방ID> <참여토큰>

```

참여자는 만든 사람이 공유한 방 ID와 참여 토큰을 사용합니다. 각 카카오 챗봇 사용자는 카카오 `botUserKey`를 기준으로 참여자로 구분됩니다. 이는 **카카오 채널 챗봇 1:1 대화** 연동입니다. 일반 카카오톡 단체방에 사용자 제작 봇 계정을 넣거나 단체방의 메시지를 읽는 API는 아닙니다. 스킬 설정 변경은 챗봇 배포 후 운영 채널에 반영됩니다.

## 회사 점심 추천 기능

카카오 챗봇 스킬에 회사 단위 점심 추천 흐름을 추가했습니다. 현재 하나의 배포 인스턴스는 하나의 회사 주소와 점심시간을 공유합니다. 회사 주소를 처음 등록한 실제 채널 사용자가 설정 관리자이므로, `회사설정`은 Skill 테스트 가짜 사용자 대신 배포된 채널 대화에서 처음 실행하세요.

- 첫 설정: `회사설정 12:00 13:00 서울 강남구 테헤란로 123` (점심 시작 12:00, 종료 13:00; 처음 설정한 사용자가 회사 설정 관리자가 됩니다)
- 회사 주소를 OpenStreetMap Nominatim으로 좌표 변환하고, Overpass에서 반경 1km 음식점 검색 (기본 설정)
- 매일 처음 조회할 때 5개 후보를 저장하고 투표 생성
- `오늘점심`은 점심 시작 전 후보와 투표수를 표시하고, 시작 시각 이후에는 최근 발견 → 투표수 → 거리 → 평가 순으로 식당을 선정
- 점심 시작 시각에 투표가 마감됩니다. `투표 2`로 1인 1표(다시 입력하면 표 변경), 점심 종료 시각 이후 `좋아요`/`싫어요`로 추천 식당을 평가합니다.
- 최근 7일 안에 시스템이 처음 발견한 식당을 최우선 추천하고, 같은 신규성 구간 안에서는 투표 수·거리·평가를 반영
- `알림켜기`를 한 사용자는 점심시간에 Kakao Event API로 결과 알림을 받을 수 있고, `알림끄기`로 해제

“최근 발견”은 실제 개업일이 아니라 이 서비스가 최근 7일 안에 처음 발견했다는 뜻입니다. 좋아요·싫어요 평가는 신규 여부·투표수·거리 동률일 때 반영됩니다. 회사 주소/점심시간 설정 시 그날 후보를 검색합니다. 장소 데이터는 © OpenStreetMap contributors이며, 지도 링크에서 상세 위치를 확인할 수 있습니다. 공개 OSM 서비스에는 사용량 제한과 가용성 제약이 있어 소규모 테스트에 적합합니다.

### Kakao 설정

기본 장소 제공자는 OpenStreetMap이며 별도 키가 필요 없습니다. 회사 주소 조회는 [Nominatim 사용 정책](https://operations.osmfoundation.org/policies/nominatim/)의 초당 1회 제한·캐시·출처 표시를 준수합니다. 음식점 후보는 공개 Overpass API에서 가져옵니다. 국내 식당 데이터가 누락될 수 있고, 공개 API 장애 시 새 후보를 검색하지 못할 수 있습니다.

카카오맵 유료 사용을 별도로 설정한 경우에만 `PLACE_PROVIDER=kakao`와 `KAKAO_REST_API_KEY`를 함께 설정합니다. 카카오맵은 개발자 계정의 첫 활성화 앱에만 무료 쿼터가 제공됩니다. [카카오맵 정책](https://developers.kakao.com/docs/ko/kakaomap/common)을 확인하세요.

점심시간 자동 푸시는 별도 선택 기능입니다. `KAKAO_BOT_ID`와 관리자센터 이벤트 이름(`KAKAO_LUNCH_EVENT_NAME`, 기본값 `lunch_recommendation`)을 설정하고, 챗봇 관리자센터의 이벤트 블록에서 같은 이름을 등록해야 합니다. 메시지 템플릿에서 `{{#current.event.data.restaurantName}}`, `address`, `distance`, `placeUrl` 값을 표시하도록 구성하세요. 사용자는 `알림켜기`로 등록해야 하며 채널 친구여야 받을 수 있습니다. 카카오 Event API는 비즈 인증 채널과 비즈 앱 연결 등 별도 선행 조건이 있습니다. [Event API 설정·발송 조건](https://kakaobusiness.gitbook.io/main/tool/chatbot/main_notions/event-api).
## 환경 변수

`.env.example`은 예시 파일이며 PowerShell이 자동으로 읽지 않습니다. OpenStreetMap 검색에는 키가 필요하지 않습니다. 카카오맵을 유료로 활성화한 경우에만 실행할 PowerShell 창에서 아래처럼 입력하세요. 카카오 푸시까지 사용할 때는 `KAKAO_BOT_ID`도 설정해야 합니다.

```powershell
$env:PLACE_PROVIDER="kakao"
$env:KAKAO_REST_API_KEY="카카오 디벨로퍼스 REST API 키"
$env:KAKAO_BOT_ID="챗봇 관리자센터 Bot ID"
$env:KAKAO_LUNCH_EVENT_NAME="lunch_recommendation"
```

`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `OWNER_SECRET`, `KAKAO_SKILL_SECRET`, `PUBLIC_BASE_URL`, `PLACE_PROVIDER` (기본 `osm`), `OSM_GEOCODER_URL`, `OSM_OVERPASS_URL`, 선택적 `KAKAO_REST_API_KEY`, `KAKAO_BOT_ID`, `KAKAO_LUNCH_EVENT_NAME`, `PORT` (기본 8080). 예제는 [.env.example](.env.example)에 있습니다.

## API 요약

| 동작 | 요청 |
|---|---|
| 방 생성 | `POST /api/rounds` `{ "title":"오늘 점심", "roomName":"점심팟", "officeAddress":"서울 강남구 테헤란로 123", "lunchStartTime":"12:00", "lunchEndTime":"13:00" }` |
| 목록/결과 | `GET /api/rounds/{id}?token=...` |
| 메뉴 등록/변경 | `PUT /api/rounds/{id}/entries?token=...` `{ "menu":"김치찌개" }` |
| 내 메뉴 취소 | `DELETE /api/rounds/{id}/entries/me?token=...` |
| 모집 시작 | `POST /api/rounds/{id}/start`, `X-Owner-Token` 헤더 |
| 예약 추첨 | 오전 10시 45분(Asia/Seoul)에 서버가 자동 확정하며, 첫 결과 조회에서도 확정 |
| 상태 확인 | `GET /actuator/health` |

생성 응답에는 방장 토큰이 포함되지만 현재 익명 룰렛 화면은 이를 사용하지 않습니다. 참여 토큰은 초대 URL을 가진 사람에게 권한을 부여하므로 링크를 공개 게시하지 마세요. 참여 식별자는 방별 임의 UUID를 브라우저 로컬 저장소에 둡니다. 계정 인증이 아닌 편의용 구분자이며, 브라우저 저장소를 지우면 기존 메뉴를 변경하거나 취소할 수 없습니다. TLS를 제공하는 HTTPS 도메인 뒤에서 운영하세요.

## 로컬 DB

```powershell
docker compose up -d db
```

Flyway는 시작 시 `V1__initial_schema.sql`을 적용하며 Hibernate는 스키마를 검증만 합니다. 서비스 구성은 [docker-compose.yml](docker-compose.yml)에 있습니다.

## 품질 및 알려진 범위

`./gradlew test`에는 익명 표시, 중복 후보 제거, 메뉴 변경·취소, 10시 45분 마감과 한 번만 확정되는 결과 테스트가 포함됩니다. 시연 화면은 실제 카카오톡이 아닌 브라우저 가상 대화입니다. 그룹 챗봇 Beta 제휴 승인 전에는 일반 카카오톡 단체방에 봇을 직접 초대할 수 없습니다.

메뉴 제출자의 실명은 요청하지 않으며 기존 저장 이름도 Flyway 마이그레이션에서 익명으로 바뀝니다. 무료 Render 인스턴스가 잠들거나 예약 작업이 지연되면 10시 45분 즉시 처리되지 않을 수 있으며, 그 후 첫 조회 때 결과를 확정합니다.

