package com.smap.domain.search.repository;

import com.smap.domain.search.model.DocType;
import com.smap.domain.search.model.DocumentHit;
import com.smap.domain.search.model.SearchDocument;
import com.smap.domain.search.model.ServicePlace;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
@RequiredArgsConstructor
public class SearchDocumentRepository {

    private static final String HIT_SQL = """
            WITH q AS (SELECT ?::text AS q, ?::text AS c, ?::vector AS v)
            SELECT s.*, CASE WHEN s.sem IS NULL THEN s.kw
                             ELSE greatest(0.85 * s.kw + 0.15 * s.sem, ? * s.sem + ? * s.kw) END AS score
            FROM (
                SELECT d.doc_type, d.ref_id, d.node_id, d.display_name, d.display_sub, d.building, d.floor_label, d.kind,
                       (SELECT coalesce(max(greatest(
                                   word_similarity(k, t.x)
                                       * (0.7 + 0.3 * least(1.0, char_length(k)::float8 / greatest(char_length(t.x), 1))),
                                   0.95 * word_similarity(t.x, k)
                                       * (0.7 + 0.3 * least(1.0, char_length(t.x)::float8 / greatest(char_length(k), 1))))), 0)
                        FROM unnest(string_to_array(d.keywords, ' | ')) k,
                             (VALUES (q.q), (q.c)) t(x)) AS kw,
                       (SELECT coalesce(max(length(k)), 0)
                        FROM unnest(string_to_array(d.keywords, ' | ')) k
                        WHERE word_similarity(k, q.q) >= 0.8) AS spec,
                       CASE WHEN q.v IS NULL OR d.embedding IS NULL THEN NULL
                            ELSE 1 - (d.embedding <=> q.v) END AS sem
                FROM public.search_documents d, q
                WHERE d.doc_type = ANY (?::text[])
            ) s
            ORDER BY score DESC, s.spec DESC, s.display_name
            LIMIT ?
            """;

    private static final String SERVICE_PLACE_SQL = """
            SELECT s.service_id, s.name AS service_name, p.place_id, p.arrive_node_id, p.name, p.sub,
                   p.building, p.floor_label, p.kind
            FROM public.service_places sp
            JOIN public.services s ON s.service_id = sp.service_id
            JOIN public.places p ON p.place_id = sp.place_id
            WHERE sp.service_id = ANY (?::bigint[]) AND p.arrive_node_id IS NOT NULL
            ORDER BY p.name, p.place_id
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO public.search_documents
                (doc_type, ref_id, node_id, display_name, display_sub, building, floor_label, kind,
                 keywords, content, content_hash)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (doc_type, ref_id) DO UPDATE SET
                node_id = EXCLUDED.node_id,
                display_name = EXCLUDED.display_name,
                display_sub = EXCLUDED.display_sub,
                building = EXCLUDED.building,
                floor_label = EXCLUDED.floor_label,
                kind = EXCLUDED.kind,
                keywords = EXCLUDED.keywords,
                content = EXCLUDED.content,
                content_hash = EXCLUDED.content_hash,
                embedding = CASE WHEN search_documents.content_hash = EXCLUDED.content_hash
                                 THEN search_documents.embedding END,
                model = CASE WHEN search_documents.content_hash = EXCLUDED.content_hash
                             THEN search_documents.model END,
                embedded_at = CASE WHEN search_documents.content_hash = EXCLUDED.content_hash
                                   THEN search_documents.embedded_at END
            """;

    private final JdbcTemplate jdbcTemplate;

    public List<DocumentHit> findHits(List<DocType> types, String query, String core, float[] vector,
                                      double semanticWeight, double keywordWeight, int limit) {
        return jdbcTemplate.query(HIT_SQL, (rs, rowNum) -> new DocumentHit(
                        DocType.valueOf(rs.getString("doc_type")),
                        rs.getString("ref_id"),
                        rs.getString("node_id"),
                        rs.getString("display_name"),
                        rs.getString("display_sub"),
                        rs.getString("building"),
                        rs.getString("floor_label"),
                        rs.getString("kind"),
                        rs.getDouble("kw"),
                        rs.getObject("sem") == null ? null : rs.getDouble("sem"),
                        rs.getDouble("score")),
                query, core, toVector(vector), semanticWeight, keywordWeight, toTextArray(types), limit);
    }

    public List<ServicePlace> findServicePlaces(Collection<String> serviceIds) {
        if (serviceIds.isEmpty()) {
            return List.of();
        }
        String ids = "{" + String.join(",", serviceIds) + "}";
        return jdbcTemplate.query(SERVICE_PLACE_SQL, (rs, rowNum) -> new ServicePlace(
                        rs.getString("service_id"),
                        rs.getString("service_name"),
                        rs.getString("place_id"),
                        rs.getString("arrive_node_id"),
                        rs.getString("name"),
                        rs.getString("sub"),
                        rs.getString("building"),
                        rs.getString("floor_label"),
                        rs.getString("kind")),
                ids);
    }

    public List<Map<String, Object>> findServices() {
        return jdbcTemplate.queryForList("""
                SELECT service_id::text AS service_id, name, description, array_to_string(keywords, ' | ') AS keywords
                FROM public.services ORDER BY service_id
                """);
    }

    public Map<String, List<String>> findServiceNamesByPlace() {
        return jdbcTemplate.queryForList("""
                        SELECT sp.place_id, s.name || coalesce(' (' || s.description || ')', '') AS label
                        FROM public.service_places sp JOIN public.services s ON s.service_id = sp.service_id
                        ORDER BY sp.place_id, s.service_id
                        """).stream()
                .collect(Collectors.groupingBy(row -> (String) row.get("place_id"),
                        Collectors.mapping(row -> (String) row.get("label"), Collectors.toList())));
    }

    public void upsert(List<SearchDocument> documents) {
        jdbcTemplate.batchUpdate(UPSERT_SQL, documents, 200, (ps, doc) -> {
            ps.setString(1, doc.docType().name());
            ps.setString(2, doc.refId());
            ps.setString(3, doc.nodeId());
            ps.setString(4, doc.displayName());
            ps.setString(5, doc.displaySub());
            ps.setString(6, doc.building());
            ps.setString(7, doc.floorLabel());
            ps.setString(8, doc.kind());
            ps.setString(9, doc.keywords());
            ps.setString(10, doc.content());
            ps.setString(11, hash(doc.content()));
        });
    }

    public int deleteMissing(DocType type, Collection<String> refIds) {
        return jdbcTemplate.update(
                "DELETE FROM public.search_documents WHERE doc_type = ? AND NOT (ref_id = ANY (?::text[]))",
                type.name(), "{" + refIds.stream().map(SearchDocumentRepository::quote).collect(Collectors.joining(",")) + "}");
    }

    public List<Map<String, Object>> findPending(List<DocType> types, boolean force) {
        return jdbcTemplate.queryForList("""
                SELECT doc_id, doc_type, content FROM public.search_documents
                WHERE doc_type = ANY (?::text[]) AND (? OR embedding IS NULL)
                ORDER BY doc_id
                """, toTextArray(types), force);
    }

    public void updateEmbeddings(List<Long> docIds, List<float[]> vectors, String model) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.batchUpdate(
                "UPDATE public.search_documents SET embedding = ?::vector, model = ?, embedded_at = ? WHERE doc_id = ?",
                IntStream.range(0, docIds.size())
                        .mapToObj(i -> new Object[]{toVector(vectors.get(i)), model, now, docIds.get(i)})
                        .toList());
    }

    public int count() {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM public.search_documents", Integer.class);
        return count == null ? 0 : count;
    }

    private static String toTextArray(List<DocType> types) {
        return "{" + types.stream().map(Enum::name).collect(Collectors.joining(",")) + "}";
    }

    private static String toVector(float[] vector) {
        if (vector == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String hash(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
