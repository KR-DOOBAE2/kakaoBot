# 점메추리알 🥚

카카오톡 일반 단체방의 공식 메시지 읽기/보내기 API는 제공되지 않으므로, 이 MVP는 **모바일 웹 참여 링크 + 카카오톡 공유**를 사용합니다. 단체방에 초대 링크를 붙여 넣고, 참여자는 웹에서 메뉴를 등록합니다. 앱은 단체방 메시지나 연락처를 수집하지 않습니다.

## 포함 기능

- 방 생성과 방장 기능 토큰 발급
- 모집 시작과 1인당 메뉴 등록·변경·취소
- 전체 참여 목록과 중복 메뉴 제거
- 중복 제거된 후보마다 동일한 확률을 주는 추첨과 직전 당첨 메뉴 제외 재추첨
- 결과 확인, 모집 마감
- 무작위 참여 토큰, DB에는 참여 토큰 SHA-256 해시만 저장, 방장 HMAC 토큰
- 참여자별 DB 유니크 제약, 방 상태 버전 기반 추첨 동시성 제어
- 모바일 귀여운 메추리알 웹 화면, 카카오톡 공유 시트(미지원 환경은 링크 복사)

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

브라우저에서 `http://localhost:8080`을 열고 방을 만든 뒤 초대 링크를 카카오톡 단체방에 공유합니다. Supabase 사용 시 프로젝트의 PostgreSQL 연결 문자열과 DB 인증 정보를 환경 변수로 설정하세요. 배포 환경은 SSL을 포함한 Supabase 연결 문자열을 사용하고 `OWNER_SECRET`을 32자 이상의 임의 비밀값으로 설정하세요. `.env`는 저장소에 커밋하지 마세요.

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

4. 블록을 만들고 사용자 발화 예시(예: `회사설정`, `오늘점심`, `투표`, `좋아요`, `싫어요`, `알림켜기`, `알림끄기`, `방 만들기`, `메뉴`, `목록`, `추첨`, `결과`)를 등록한 다음, 해당 블록의 **스킬 응답**에 새 스킬을 연결합니다. 응답을 스킬 데이터로 사용하도록 설정하고 저장합니다.
5. 챗봇 테스트에서 `방 만들기 우리팀 점심`을 보내고 응답의 방 ID와 참여/방장 토큰을 확인합니다. 같은 채널 챗봇 대화에서 다음 명령을 입력합니다.

```text
메뉴 <방ID> <참여토큰> 민지 김치찌개
목록 <방ID> <참여토큰>
추첨 <방ID> <방장토큰>
재추첨 <방ID> <방장토큰>
결과 <방ID> <참여토큰>
취소 <방ID> <참여토큰>
마감 <방ID> <방장토큰>
```

참여자는 만든 사람이 공유한 방 ID와 참여 토큰을 사용합니다. 각 카카오 챗봇 사용자는 카카오 `botUserKey`를 기준으로 참여자로 구분됩니다. 이는 **카카오 채널 챗봇 1:1 대화** 연동입니다. 일반 카카오톡 단체방에 사용자 제작 봇 계정을 넣거나 단체방의 메시지를 읽는 API는 아닙니다. 스킬 설정 변경은 챗봇 배포 후 운영 채널에 반영됩니다.

## 회사 점심 추천 기능

카카오 챗봇 스킬에 회사 단위 점심 추천 흐름을 추가했습니다. 현재 하나의 배포 인스턴스는 하나의 회사 주소와 점심시간을 공유합니다. 회사 주소를 처음 등록한 실제 채널 사용자가 설정 관리자이므로, `회사설정`은 Skill 테스트 가짜 사용자 대신 배포된 채널 대화에서 처음 실행하세요.

- 첫 설정: `회사설정 12:00 13:00 서울 강남구 테헤란로 123` (점심 시작 12:00, 종료 13:00; 처음 설정한 사용자가 회사 설정 관리자가 됩니다)
- 회사 주소를 Kakao Local API로 좌표 변환 후, 반경 1km 음식점 검색
- 매일 처음 조회할 때 5개 후보를 저장하고 투표 생성
- `오늘점심`은 점심 시작 전 후보와 투표수를 표시하고, 시작 시각 이후에는 최근 발견된 식당을 우선한 뒤 투표수로 추천
- 점심 시작 시각에 투표가 마감됩니다. `투표 2`로 1인 1표(다시 입력하면 표 변경), 점심 종료 시각 이후 `좋아요`/`싫어요`로 추천 식당을 평가합니다.
- 최근 7일 안에 시스템이 처음 발견한 식당을 최우선 추천하고, 같은 신규성 구간 안에서는 투표 수·평가·거리를 반영
- `알림켜기`를 한 사용자는 점심시간에 Kakao Event API로 결과 알림을 받을 수 있고, `알림끄기`로 해제

실제 신규 개업일 데이터는 Kakao Local 장소 검색 응답에 포함되지 않아, “신규”는 이 서비스가 최근 7일 안에 처음 발견한 식당을 뜻합니다. 좋아요 평가는 투표 동률 추천 우선순위에 반영합니다. 회사 주소/점심시간 설정 시 그날 후보를 검색합니다.

### Kakao 설정

Kakao Developers에서 Local API용 REST API 키를 발급해 `KAKAO_REST_API_KEY`에 넣어야 실제 주소/음식점을 검색합니다. [카카오 장소 검색 API](https://developers.kakao.com/docs/ko/local/dev-guide)는 `Authorization: KakaoAK` REST 키로 음식점 카테고리(FD6)를 검색합니다.

점심시간 자동 푸시는 별도 선택 기능입니다. `KAKAO_BOT_ID`와 관리자센터 이벤트 이름(`KAKAO_LUNCH_EVENT_NAME`, 기본값 `lunch_recommendation`)을 설정하고, 챗봇 관리자센터의 이벤트 블록에서 같은 이름을 등록해야 합니다. 메시지 템플릿에서 `{{#current.event.data.restaurantName}}`, `address`, `distance`, `placeUrl` 값을 표시하도록 구성하세요. 사용자는 `알림켜기`로 등록해야 하며 채널 친구여야 받을 수 있습니다. 카카오 Event API는 비즈 인증 채널과 비즈 앱 연결 등 별도 선행 조건이 있습니다. [Event API 설정·발송 조건](https://kakaobusiness.gitbook.io/main/tool/chatbot/main_notions/event-api).
## 환경 변수

`.env.example`은 예시 파일이며 PowerShell이 자동으로 읽지 않습니다. 실행할 PowerShell 창에서 카카오 키를 설정하려면 아래처럼 입력하세요. 카카오 푸시까지 사용할 때는 `KAKAO_BOT_ID`도 설정해야 합니다.

```powershell
$env:KAKAO_REST_API_KEY="카카오 디벨로퍼스 REST API 키"
$env:KAKAO_BOT_ID="챗봇 관리자센터 Bot ID"
$env:KAKAO_LUNCH_EVENT_NAME="lunch_recommendation"
```

`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `OWNER_SECRET`, `KAKAO_SKILL_SECRET`, `PUBLIC_BASE_URL`, `KAKAO_REST_API_KEY`, `KAKAO_BOT_ID`, `KAKAO_LUNCH_EVENT_NAME`, `PORT` (기본 8080). 예제는 [.env.example](.env.example)에 있습니다.

## API 요약

| 동작 | 요청 |
|---|---|
| 방 생성 | `POST /api/rounds` `{ "title":"오늘 점심", "roomName":"점심팟" }` |
| 목록/결과 | `GET /api/rounds/{id}?token=...` |
| 메뉴 등록/변경 | `PUT /api/rounds/{id}/entries?token=...` `{ "participantName":"민지", "menu":"김치찌개" }` |
| 내 메뉴 취소 | `DELETE /api/rounds/{id}/entries/me?token=...` |
| 모집 시작 | `POST /api/rounds/{id}/start`, `X-Owner-Token` 헤더 |
| 추첨/재추첨/마감 | `POST /api/rounds/{id}/draw`, `/reroll`, `/close`, 같은 방장 헤더 |
| 상태 확인 | `GET /actuator/health` |

생성 응답의 방장 토큰은 브라우저 로컬 저장소에 보관됩니다. 참여 토큰은 초대 URL을 가진 사람에게 권한을 부여하므로 링크를 공개 게시하지 마세요. 참여 식별자는 방별 임의 UUID를 브라우저 로컬 저장소에 둡니다. 계정 인증이 아닌 편의용 구분자이며, 브라우저 저장소를 지우면 기존 메뉴를 변경하거나 취소할 수 없습니다. TLS를 제공하는 HTTPS 도메인 뒤에서 운영하세요.

## 로컬 DB

```powershell
docker compose up -d db
```

Flyway는 시작 시 `V1__initial_schema.sql`을 적용하며 Hibernate는 스키마를 검증만 합니다. 서비스 구성은 [docker-compose.yml](docker-compose.yml)에 있습니다.

## 품질 및 알려진 범위

`./gradlew test`에는 메뉴 중복, 재추첨, 참여자 메뉴 변경/취소, 잘못된 토큰, 마감 뒤 변경 금지 테스트가 포함됩니다. 원격 Supabase 계정이나 카카오톡 일반 단체방 메시지 API는 요구하지 않습니다. 참가자는 표시 이름을 직접 입력하며 계정 인증은 없습니다. 앱 인스턴스 재시작 뒤에도 방장 토큰은 OWNER_SECRET이 유지되는 동안 재생성할 수 있지만, 브라우저 데이터를 지우면 원래 만든 사람 확인 기능은 없습니다. 이 초기판은 방장 복구 UI, 속도 제한, 계정 로그인, 링크 만료를 제공하지 않으므로 신뢰하는 소규모 방에서 사용하세요.







