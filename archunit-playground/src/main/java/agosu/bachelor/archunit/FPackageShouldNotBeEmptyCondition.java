package agosu.bachelor.archunit;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;

import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;

public class FPackageShouldNotBeEmptyCondition extends ArchCondition<JavaClass> {

    private final CustomArchitectures.FunctionalArchitecture.FPackageDefinition fPackageDefinition;
    private boolean empty = true;

    FPackageShouldNotBeEmptyCondition(final CustomArchitectures.FunctionalArchitecture.FPackageDefinition fPackageDefinition) {
        super("not be empty");
        this.fPackageDefinition = fPackageDefinition;
    }

    @Override
    public void check(JavaClass item, ConditionEvents events) {
        empty = false;
    }

    @Override
    public void finish(ConditionEvents events) {
        if (empty) {
            events.add(
                    violated(
                            fPackageDefinition,
                            String.format("FPackage '%s' is empty", fPackageDefinition.getName())
                    )
            );
        }
    }

}
