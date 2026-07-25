package com.easybeach.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.shared.tenancy.TenantScoped;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Verifica las reglas de dependencia entre módulos de ADR-002 y la
 * convención {@code @TenantScoped} de ADR-001. Al día de hoy la mayoría de
 * los módulos (branding, catalog, stay, ordering, payments, concierge,
 * promotions, reporting) están vacíos - construidos en etapas 10-15 - pero
 * la regla queda activa desde la etapa 09 para que ninguna dependencia
 * indebida se cuele a medida que se completan.
 */
class ModuleDependencyRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.easybeach");

    /** Módulo -> otros módulos de los que puede depender (ADR-002). "shared" siempre está permitido. */
    private static final Map<String, Set<String>> ALLOWED_DEPENDENCIES = Map.ofEntries(
            Map.entry("shared", Set.of()),
            Map.entry("identity", Set.of()),
            Map.entry("platform", Set.of("identity")),
            Map.entry("branding", Set.of("platform")),
            Map.entry("catalog", Set.of("platform")),
            Map.entry("stay", Set.of("identity", "platform")),
            Map.entry("ordering", Set.of("stay", "catalog", "promotions", "payments")),
            Map.entry("payments", Set.of("platform")),
            Map.entry("concierge", Set.of("stay")),
            Map.entry("promotions", Set.of("catalog")),
            Map.entry("reporting", Set.of())
    );

    @Test
    void modulosSoloDependenDeLosModulosDeclaradosEnAdr002() {
        for (var entry : ALLOWED_DEPENDENCIES.entrySet()) {
            String module = entry.getKey();
            Set<String> allowed = entry.getValue();
            for (String other : ALLOWED_DEPENDENCIES.keySet()) {
                boolean permitido = other.equals(module) || other.equals("shared") || allowed.contains(other);
                if (permitido) {
                    continue;
                }
                ArchRule rule = noClasses()
                        .that().resideInAPackage("com.easybeach." + module + "..")
                        .should().dependOnClassesThat().resideInAPackage("com.easybeach." + other + "..");
                rule.check(CLASSES);
            }
        }
    }

    @Test
    void entidadesConBalnearioIdDebenEstarAnotadasTenantScoped() {
        for (JavaClass javaClass : CLASSES) {
            if (!javaClass.isAnnotatedWith(Entity.class)) {
                continue;
            }
            boolean tieneBalnearioId = javaClass.getFields().stream()
                    .anyMatch(field -> field.getName().equals("balnearioId"));
            if (tieneBalnearioId) {
                assertThat(javaClass.isAnnotatedWith(TenantScoped.class))
                        .as("%s tiene balnearioId pero no está anotada @TenantScoped (ADR-001)", javaClass.getName())
                        .isTrue();
            }
        }
    }
}
