-- 표시용 건물명은 보존하고 프론트 식별용 고정 ID를 추가한다.
ALTER TABLE nodes ADD COLUMN building_id text;
ALTER TABLE places ADD COLUMN building_id text;

-- 마이그레이션 세션에서만 사용하는 매핑 함수. 건물 표시명 변경 시 ID를 재생성하지 않는다.
CREATE FUNCTION pg_temp.smap_building_id(name text, floor text) RETURNS text
LANGUAGE sql IMMUTABLE AS $$
    SELECT CASE name
        WHEN '본관' THEN 'BON'
        WHEN '북악관' THEN 'BUKAK'
        WHEN '청운관' THEN 'CHUNGWOON'
        WHEN '대일관' THEN 'DAEIL'
        WHEN '은주1관' THEN 'EUNJU1'
        WHEN '은주2관' THEN 'EUNJU2'
        WHEN '혜인관' THEN 'HYEIN'
        WHEN '한림관' THEN 'HANLIM'
        WHEN '문예관' THEN 'MOONYE'
        WHEN '상승관' THEN 'SANGSEUNG'
        WHEN '유담관' THEN 'YUDAM'
        WHEN '수인관' THEN 'SUIN'
        WHEN '문예관-북악관 연결구간' THEN 'MOONYE_BUKAK_CONNECTOR'
        WHEN '야외' THEN CASE
            WHEN btrim(floor) = '지하' OR upper(btrim(floor)) ~ '^B[0-9]+(F|층)?$'
                OR btrim(floor) ~ '^지하[[:space:]]*[0-9]+(층)?$'
                OR btrim(floor) ~ '^-[0-9]+(층)?$' THEN 'OUTDOOR_UNDERGROUND'
            WHEN btrim(floor) IN ('지상', '야외')
                OR upper(btrim(floor)) ~ '^([0-9]+|L)(층)?$' THEN 'OUTDOOR_GROUND'
            ELSE 'OUTDOOR_UNKNOWN'
        END
        ELSE NULL
    END
$$;

UPDATE nodes SET building_id = pg_temp.smap_building_id(building, floor_label);
UPDATE places p SET building_id = pg_temp.smap_building_id(p.building,
    COALESCE(NULLIF(btrim(p.floor_label), ''),
        (SELECT n.floor_label FROM nodes n WHERE n.node_id = p.arrive_node_id)));

-- 등록되지 않은 건물명은 추정하지 않고 전체 작업을 롤백한다.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM nodes WHERE building_id IS NULL)
        OR EXISTS (SELECT 1 FROM places WHERE building_id IS NULL) THEN
        RAISE EXCEPTION '정의되지 않은 건물명이 있습니다. 고정 ID 대응표를 확인하세요.';
    END IF;
END $$;
ALTER TABLE nodes ALTER COLUMN building_id SET NOT NULL;
ALTER TABLE places ALTER COLUMN building_id SET NOT NULL;
CREATE INDEX idx_nodes_building_id ON nodes(building_id);
CREATE INDEX idx_places_building_id ON places(building_id);
DROP FUNCTION pg_temp.smap_building_id(text, text);
