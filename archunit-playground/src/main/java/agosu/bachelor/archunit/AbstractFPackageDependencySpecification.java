package agosu.bachelor.archunit;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.lang.ArchRule;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public abstract class AbstractFPackageDependencySpecification {

    private final String fPackageName;
    protected final Set<String> allowedFPackages = new LinkedHashSet<>();
    protected FPackageDependencyConstraint constraint;
    protected String descriptionSuffix;

    AbstractFPackageDependencySpecification(String fPackageName) {
        this.fPackageName = fPackageName;
        this.descriptionSuffix = "may not access any FPackages";
    }

    public abstract CustomArchitectures.FunctionalArchitecture denyFPackageAccess(FPackageDependencyConstraint constraint, String description);

    public abstract CustomArchitectures.FunctionalArchitecture restrictFPackages(FPackageDependencyConstraint constraint, String[] fPackageNames, String descriptionTemplate);

    /**
     * Forbids any {@link Dependency dependency} from another FPackage to this FPackage.
     * @return a {@link CustomArchitectures.FunctionalArchitecture} to be used as an {@link ArchRule} or further restricted through a fluent API.
     */
    @PublicAPI(usage = ACCESS)
    public CustomArchitectures.FunctionalArchitecture mayNotBeAccessedByAnyFPackage() {
        return denyFPackageAccess(FPackageDependencyConstraint.ORIGIN, "may not be accessed by any FPackage");
    }

    /**
     * Restricts this FPackage to only allow the specified FPackages to have {@link Dependency dependencies} to this FPackage.
     * @param fPackageNames the names of other FPackages that may access this FPackage
     * @return a {@link CustomArchitectures.FunctionalArchitecture} to be used as an {@link ArchRule} or further restricted through a fluent API.
     */
    @PublicAPI(usage = ACCESS)
    public CustomArchitectures.FunctionalArchitecture mayOnlyBeAccessedByFPackages(String... fPackageNames) {
        return restrictFPackages(FPackageDependencyConstraint.ORIGIN, fPackageNames, "may only be accessed by FPackages ['%s']");
    }

    /**
     * Forbids any {@link Dependency dependency} from this FPackage to any other FPackage.
     * @return a {@link CustomArchitectures.FunctionalArchitecture} to be used as an {@link ArchRule} or further restricted through a fluent API.
     */
    @PublicAPI(usage = ACCESS)
    public CustomArchitectures.FunctionalArchitecture mayNotAccessAnyFPackage() {
        return denyFPackageAccess(FPackageDependencyConstraint.TARGET, "may not access any FPackage");
    }

    /**
     * Restricts this FPackage to only allow {@link Dependency dependencies} to the specified FPackages.
     * @param fPackageNames the only names of other FPackages that this FPackage may access
     * @return a {@link CustomArchitectures.FunctionalArchitecture} to be used as an {@link ArchRule} or further restricted through a fluent API.
     */
    @PublicAPI(usage = ACCESS)
    public CustomArchitectures.FunctionalArchitecture mayOnlyAccessFPackages(String... fPackageNames) {
        return restrictFPackages(FPackageDependencyConstraint.TARGET, fPackageNames, "may only access FPackages ['%s']");
    }

    @Override
    public String toString() {
        return String.format("where FPackage '%s' %s", fPackageName, descriptionSuffix);
    }

    public String getFPackageName() {
        return this.fPackageName;
    }

}
