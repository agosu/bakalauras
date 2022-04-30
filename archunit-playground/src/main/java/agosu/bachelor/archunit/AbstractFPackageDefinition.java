package agosu.bachelor.archunit;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

import java.util.Arrays;

import static agosu.bachelor.archunit.Utils.getPackageExcludingSubpackages;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class AbstractFPackageDefinition {

    private final String name;
    protected String thePackage;
    protected DescribedPredicate<JavaClass> containsPredicate;
    protected DescribedPredicate<JavaClass> excludeSubpackagePredicate;

    AbstractFPackageDefinition(String name) {
        checkState(!isNullOrEmpty(name), "FPackage name must be present");
        this.name = name;
    }

    public abstract CustomArchitectures.FunctionalArchitecture definedBy(
            String thePackage,
            DescribedPredicate<? super JavaClass> containsPredicate,
            DescribedPredicate<? super JavaClass> excludeSubpackagePredicate);

    @PublicAPI(usage = ACCESS)
    public CustomArchitectures.FunctionalArchitecture definedBy(String... packageIdentifiers) {
        String oneAndOnly = Arrays.stream(packageIdentifiers).findFirst().get();
        String excludeSubPackageIdentifier = getPackageExcludingSubpackages(oneAndOnly);
        return definedBy(
                oneAndOnly,
                resideInAnyPackage(packageIdentifiers),
                resideInAnyPackage(excludeSubPackageIdentifier));
    }

    DescribedPredicate<JavaClass> containsPredicate() {
        return containsPredicate;
    }

    DescribedPredicate<JavaClass> excludeSubpackagePredicate() {
        return excludeSubpackagePredicate;
    }

    @Override
    public String toString() {
        return String.format("FPackage '%s' (%s)", name, containsPredicate);
    }

    public String getName() {
        return this.name;
    }

    public String getThePackage() {
        return this.thePackage;
    }

}
