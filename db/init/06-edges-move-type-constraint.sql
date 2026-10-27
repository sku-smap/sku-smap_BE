-- 기존 DB용 마이그레이션. 배포의 db/init/*.sql 순차 실행에도 포함된다.
-- 빈 이동 유형은 미확정 값으로 유지한다. 간선 삭제나 이동 유형 추정은 하지 않는다.
BEGIN;
LOCK TABLE public.edges IN SHARE ROW EXCLUSIVE MODE;

DO $$
BEGIN
    -- 알려진 유형의 공백·대소문자만 수정한다. 미지의 유형은 현장 확인 없이 바꾸지 않는다.
    IF EXISTS (
        SELECT 1 FROM public.edges
        WHERE upper(btrim(move_type, E' \t\n\r')) NOT IN ('', 'WALK', 'STAIRS', 'ELEVATOR', 'DOOR')
    ) THEN
        RAISE EXCEPTION '알 수 없는 move_type이 있습니다. 값을 확인한 뒤 마이그레이션을 다시 실행하세요.';
    END IF;

    -- 복합 기본키가 겹치면 임의 병합·삭제 없이 전체 작업을 중단한다.
    IF EXISTS (
        SELECT 1 FROM public.edges
        GROUP BY from_node_id, to_node_id, upper(btrim(move_type, E' \t\n\r'))
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'move_type 정규화 후 기본키 충돌이 발생합니다. 간선을 확인하세요.';
    END IF;
END $$;

UPDATE public.edges
SET move_type = upper(btrim(move_type, E' \t\n\r'))
WHERE move_type IS DISTINCT FROM upper(btrim(move_type, E' \t\n\r'));

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.edges'::regclass AND conname = 'edges_move_type_check'
    ) THEN
        ALTER TABLE public.edges ADD CONSTRAINT edges_move_type_check
            CHECK (move_type IN ('', 'WALK', 'STAIRS', 'ELEVATOR', 'DOOR')) NOT VALID;
    END IF;
END $$;

ALTER TABLE public.edges VALIDATE CONSTRAINT edges_move_type_check;
COMMIT;
