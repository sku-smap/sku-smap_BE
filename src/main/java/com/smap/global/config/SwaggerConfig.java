package com.smap.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smap API")
                        .description("Smap API 문서")
                        .version("v1"));
    }

    /** 공통 enum·제네릭 참조에서 swagger-core가 누락한 nullable 정보를 보완한다. */
    @Bean
    public OpenApiCustomizer nullableReferenceFields() {
        return api -> {
            if (api.getComponents() == null || api.getComponents().getSchemas() == null) return;
            var schemas = api.getComponents().getSchemas();
            for (String name : List.of("PlaceResponseDTO", "PlaceDTO", "CandidateDTO")) {
                nullableField(schemas.get(name), "kind");
            }
            schemas.forEach((name, schema) -> {
                if (name.startsWith("ApiResponse")) nullableField(schema, "data");
            });
        };
    }

    /** $ref 옆의 속성은 무시될 수 있으므로 allOf로 감싸 null 허용을 명시한다. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void nullableField(Schema owner, String field) {
        if (owner == null) return;
        Schema property = owner.getProperties() == null ? null : (Schema) owner.getProperties().get(field);
        if (property == null) {
            // ApiResponse<Void>도 실제 JSON에서는 data: null 키를 항상 반환한다.
            owner.addProperty(field, new Schema<>().nullable(true));
        } else if (property.get$ref() != null) {
            var wrapper = new ComposedSchema();
            wrapper.addAllOfItem(property);
            wrapper.setNullable(true);
            owner.addProperty(field, wrapper);
        } else {
            property.setNullable(true);
        }
        if (owner.getRequired() == null || !owner.getRequired().contains(field)) owner.addRequiredItem(field);
    }
}
