BEGIN;

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS public.services (
    service_id bigint PRIMARY KEY,
    name text NOT NULL,
    keywords text[] NOT NULL DEFAULT '{}',
    description text
);

CREATE TABLE IF NOT EXISTS public.service_places (
    service_id bigint NOT NULL REFERENCES public.services(service_id) ON DELETE CASCADE,
    place_id text NOT NULL REFERENCES public.places(place_id) ON DELETE CASCADE,
    note text,
    PRIMARY KEY (service_id, place_id)
);

CREATE INDEX IF NOT EXISTS idx_service_places_place ON public.service_places(place_id);

CREATE TABLE IF NOT EXISTS public.search_documents (
    doc_id bigserial PRIMARY KEY,
    doc_type text NOT NULL CHECK (doc_type IN ('PLACE', 'NODE', 'SERVICE')),
    ref_id text NOT NULL,
    node_id text,
    display_name text NOT NULL,
    display_sub text,
    building text,
    floor_label text,
    kind text,
    keywords text NOT NULL,
    content text NOT NULL,
    content_hash text NOT NULL,
    embedding vector(1024),
    model text,
    embedded_at timestamp,
    UNIQUE (doc_type, ref_id)
);

CREATE INDEX IF NOT EXISTS idx_search_documents_embedding
    ON public.search_documents USING hnsw (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS public.search_log (
    log_id bigserial PRIMARY KEY,
    target text NOT NULL,
    input_type text NOT NULL,
    query_text text NOT NULL,
    normalized_query text NOT NULL,
    transcript text,
    stt_engine text,
    route_option text,
    status text NOT NULL,
    candidate_ids text,
    selected_candidate_id text,
    latency_ms integer,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);

COMMIT;
