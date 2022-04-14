import agosu.bachelor.archunit.CustomArchitectures;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static agosu.bachelor.archunit.Constants.SYSTEM_PATH;
import static agosu.bachelor.archunit.CustomArchitectures.functionalArchitecture;

import static agosu.bachelor.archunit.CustomPredicates.*;

public class FunctionalArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages(SYSTEM_PATH);

    @Test
    public void some_architecture_rule_1() {
        getArchitecture()
                .whereDependencyDirectionUp()
                .whereFPackage("users").mayOnlyAccessFPackages("books")
                .check(classes);
    }

    private CustomArchitectures.FunctionalArchitecture getArchitecture() {
        return functionalArchitecture()
                .ignoreDependency(isInsideThisSystem(SYSTEM_PATH), isOutsideThisSystem(SYSTEM_PATH))
                .fPackage("books").definedBy("com.library.domain.books..")
                .fPackage("events").definedBy("com.library.domain.events..")
                .fPackage("users").definedBy("com.library.domain.users..")
                .fPackage("email").definedBy("com.library.infrastructure.email..")
                .fPackage("pdf").definedBy("com.library.infrastructure.pdf..")
                .whereGroup("com.library.domain")
                .whereGroup("com.library.infrastructure");
    }
}
