import agosu.bachelor.archunit.CustomArchitectures;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.Test;

import static agosu.bachelor.archunit.CustomArchitectures.functionalArchitecture;
import static agosu.bachelor.archunit.CustomPredicates.*;

public class FunctionalArchitectureTest {

    private final String SYSTEM_PATH = "com.library";
    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages(SYSTEM_PATH);

    @Test
    public void some_architecture_rule_1() {
        getArchitecture()
                .whereDependencyDirectionUp()
                .whereFPackage("users").mayOnlyAccessFPackages("books", "email")
                .check(classes);
    }

    @Test
    public void some_architecture_rule_2() {
        getArchitecture()
                .whereDependencyDirectionUp()
                .check(classes);
    }

    private CustomArchitectures.FunctionalArchitecture getArchitecture() {
        return functionalArchitecture()
                .systemRoot(SYSTEM_PATH)
                .ignoreDependency(isInsideThisSystem(SYSTEM_PATH), isOutsideThisSystem(SYSTEM_PATH))
                .fPackage("books").definedBy("com.library.domain.books..")
                .fPackage("events").definedBy("com.library.domain.events..")
                .fPackage("users").definedBy("com.library.domain.users..")
                .fPackage("email").definedBy("com.library.infrastructure.email..")
                .fPackage("pdf").definedBy("com.library.infrastructure.pdf..")
                .group("com.library.domain")
                .group("com.library.infrastructure");
    }
}
