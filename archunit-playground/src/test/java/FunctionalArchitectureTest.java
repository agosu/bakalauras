import agosu.bachelor.archunit.CustomArchitectures;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.Test;

import static agosu.bachelor.archunit.CustomArchitectures.functionalArchitecture;

import static agosu.bachelor.archunit.CustomConditions.*;
import static agosu.bachelor.archunit.CustomTransformers.packages;
import static agosu.bachelor.archunit.CustomPredicates.*;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

public class FunctionalArchitectureTest {

    private final String SYSTEM_PATH = "com.library";
    private final JavaClasses classes = new ClassFileImporter().importPackages(SYSTEM_PATH);

    @Test
    public void some_architecture_rule_1() {
        getArchitecture()
                .whereDependencyDirectionUp()
                //.whereFPackage("books").mayNotAccessAnyFPackage()
                //.whereFPackage("users").mayOnlyAccessFPackages("books")
                .check(classes);
    }

    @Test
    public void some_architecture_rule_2() {
        classes().that().haveSimpleNameContaining("Users").should(notHaveFieldNamedTest).check(classes);
    }

    @Test
    public void some_architecture_rule_3() {
        all(packages).that(areDirectRootChildrenOf(SYSTEM_PATH)).should(notBeLayers).check(classes);
    }

    @Test
    public void some_architecture_rule_4() {
        //classes().should(accessClassesInTheSameOrDirectParentPackageOrDirectSubpackage).check(classes);
        //classes().should(accessClassesInTheSamePackage).orShould(accessClassesInDirectParentPackage).orShould(accessClassesInDirectSubpackage).check(classes);
        //classes().should(accessClassesInTheSamePackage.or(accessClassesInDirectParentPackage).or(accessClassesInDirectSubpackage)).check(classes);
        //noClasses().should(accessClassesNotInTheSamePackage).andShould(accessClassesNotInDirectParentPackage).andShould(accessClassesNotInDirectSubpackage).check(classes);
        classes().should(accessClassesInUpperLayerOfASiblingPackage).check(classes);
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
