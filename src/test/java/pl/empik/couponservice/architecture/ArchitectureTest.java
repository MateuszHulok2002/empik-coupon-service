package pl.empik.couponservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes =
                new ClassFileImporter()
                        .withImportOption(new ImportOption.DoNotIncludeTests())
                        .importPackages("pl.empik.couponservice");
    }

    @Test
    void domainMustRemainFrameworkIndependent() {
        noClasses()
                .that()
                .resideInAPackage("..coupon.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.servlet..",
                        "..coupon.api..",
                        "..coupon.application..",
                        "..coupon.infrastructure..")
                .check(classes);
    }

    @Test
    void applicationMustNotDependOnAdaptersOrJpa() {
        noClasses()
                .that()
                .resideInAPackage("..coupon.application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..coupon.api..",
                        "..coupon.infrastructure..",
                        "jakarta.persistence..",
                        "jakarta.servlet..")
                .check(classes);
    }

    @Test
    void adaptersMustRespectDependencyDirection() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain")
                .definedBy("..coupon.domain..")
                .layer("Application")
                .definedBy("..coupon.application..")
                .layer("API")
                .definedBy("..coupon.api..")
                .layer("Infrastructure")
                .definedBy("..coupon.infrastructure..")
                .whereLayer("Domain")
                .mayOnlyBeAccessedByLayers("Application", "API", "Infrastructure")
                .whereLayer("Application")
                .mayOnlyBeAccessedByLayers("API", "Infrastructure")
                .whereLayer("API")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Infrastructure")
                .mayNotBeAccessedByAnyLayer()
                .check(classes);
    }

    @Test
    void featurePackagesMustBeFreeOfCycles() {
        slices()
                .matching("pl.empik.couponservice.coupon.(*)..")
                .should()
                .beFreeOfCycles()
                .check(classes);
    }
}
