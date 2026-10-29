# 장소 응답 필드

`GET /api/v1/places`, `/campus-map` 및 `/routes` 하위 places[]는 같은 별명·분류 정책을 사용한다.
AI 검색 후보의 kind도 동일한 enum을 사용한다.

```json
{
  "placeId": "r-eunju1-202",
  "name": "도시공학과 실습실 (202호)",
  "kind": "room",
  "arriveNodeId": "EJ1_02_E",
  "aliases": ["도시공학과 실습실", "202호", "은주1관 202"],
  "sub": "은주1관 2층 202호"
}
```

## 별명

aliases는 항상 문자열 배열이다. DB의 원본 문자열을 `|`로 분리하고 항목 양 끝 공백,
빈 항목, 정확히 일치하는 중복을 제거한다. 순서는 최초 등장 순서이다.
원본이 null·공백·구분자뿐이면 빈 배열 `[]`을 반환한다.
쉼표는 구분자가 아니며, 영문 대소문자와 별명 내용은 보존한다.
DB 컬럼·저장값·검색 방식은 변경하지 않는다. 프론트에서 재분리할 필요가 없다.

## 분류

Java PlaceKind enum을 사용하며 JSON·Swagger에는 기존 소문자 값을 유지한다.

| 값 | 의미 |
| --- | --- |
| room | 강의실·실습실 등 호실 |
| professor | 교수 연구실 |
| office | 사무실·행정실 |
| facility | 편의·공용 시설 |
| building | 건물 대표 장소 |

원본 분류가 null·공백이면 kind는 null이다. 등록되지 않은 분류는 임의의 enum으로 대체하지 않고 변환 오류로 처리한다.
DB kind 컬럼은 문자열을 유지하며 이번 변경은 DB 제약을 추가하는 마이그레이션을 포함하지 않는다.

## 필수값·보조 설명

- placeId: DB 기본키이며 NOT NULL이다.
- name: DB에서 NOT NULL이다. 공백 문자열을 막는 별도 CHECK 제약은 없다.
- arriveNodeId: DB에서는 null을 허용하지만 GET /places는 실제 노드에 연결된 장소만 반환한다.
- sub: DB의 자유 형식 보조 설명을 그대로 제공한다. 값이 없으면 null이며 백엔드가 자동 생성하지 않는다.
  프론트는 sub가 없으면 building·floorLabel·roomNo 중 값이 있는 항목으로 조합할 수 있다.
- 장소 사진은 응답에 포함하지 않는다.
