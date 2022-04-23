package agosu.bachelor.archunit;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.*;
import com.tngtech.archunit.lang.syntax.PredicateAggregator;
import com.google.common.base.Joiner;

import java.util.*;

import static agosu.bachelor.archunit.CustomPredicates.areDirectRootChildrenOf;
import static agosu.bachelor.archunit.CustomPredicates.areInParentPackageOf;
import static agosu.bachelor.archunit.CustomTransformers.packages;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.*;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependenciesWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependentsWhere;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.all;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.newArrayList;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import static agosu.bachelor.archunit.CustomConditions.*;

public final class CustomArchitectures {

    private CustomArchitectures() {}

    @PublicAPI(usage = ACCESS)
    public static FunctionalArchitecture functionalArchitecture() {
        return new FunctionalArchitecture();
    }

    public static final class FunctionalArchitecture implements ArchRule {

        private final FPackageDefinitions fPackageDefinitions;
        private final Set<FPackageDependencySpecification> dependencySpecifications;
        private final PredicateAggregator<Dependency> irrelevantDependenciesPredicate;
        private final DependencyDirection dependencyDirection;
        private final Set<String> groups;
        private final String systemRoot;

        private final boolean fPackagesOn;

        private FunctionalArchitecture() {
            this(
                    new FPackageDefinitions(),
                    new LinkedHashSet<>(),
                    new PredicateAggregator<Dependency>().thatORs(),
                    DependencyDirection.BOTH,
                    new LinkedHashSet<>(),
                    "",
                    true
            );
        }

        private FunctionalArchitecture(
                FPackageDefinitions fPackageDefinitions,
                Set<FPackageDependencySpecification> dependencySpecifications,
                PredicateAggregator<Dependency> irrelevantDependenciesPredicate,
                DependencyDirection dependencyDirection,
                Set<String> groups,
                String systemRoot,
                boolean fPackagesOn) {
            this.fPackageDefinitions = fPackageDefinitions;
            this.dependencySpecifications = dependencySpecifications;
            this.irrelevantDependenciesPredicate = irrelevantDependenciesPredicate;
            this.dependencyDirection = dependencyDirection;
            this.groups = groups;
            this.systemRoot = systemRoot;
            this.fPackagesOn = fPackagesOn;
        }

        @PublicAPI(usage = ACCESS)
        public FunctionalArchitecture inFunctionalArchitecture() {
            return new FunctionalArchitecture(
                    fPackageDefinitions,
                    dependencySpecifications,
                    irrelevantDependenciesPredicate,
                    dependencyDirection,
                    groups,
                    systemRoot,
                    fPackagesOn
            );
        }

        private FunctionalArchitecture addFPackageDefinition(FPackageDefinition definition) {
            fPackageDefinitions.add(definition);
            return this;
        }

        private FunctionalArchitecture addDependencySpecification(FPackageDependencySpecification dependencySpecification) {
            dependencySpecifications.add(dependencySpecification);
            return this;
        }
        
        @PublicAPI(usage = ACCESS)
        public FPackageDefinition fPackage(String name) {
            return new FPackageDefinition(name);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public String getDescription() {
            List<String> lines = newArrayList("Functional architecture consisting of");
            for (FPackageDefinition definition : fPackageDefinitions) {
                lines.add(definition.toString());
            }
            for (FPackageDependencySpecification specification : dependencySpecifications) {
                lines.add(specification.toString());
            }

            return Joiner.on(lineSeparator()).join(lines);
        }

        @Override
        public String toString() {
            return getDescription();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public EvaluationResult evaluate(JavaClasses classes) {
            EvaluationResult result = new EvaluationResult(this, Priority.MEDIUM);
            checkEmptyFPackages(classes, result);
            checkNoUpperLayerPackageIsLayer(classes, result);
            if (fPackagesOn) {
                checkAllClassesBelongToFPackagesOrAreDirectGroupChildren(classes, result);
            }
            switch (this.dependencyDirection) {
                case UP:
                    checkDependencyDirectionUp(classes, result);
                    break;
                case DOWN:
                    checkDependencyDirectionDown(classes, result);
                    break;
                default:
                    break;
            }
            checkCyclicDependenciesBetweenPackages(classes, result);
            for (FPackageDependencySpecification specification : dependencySpecifications) {
                result.add(evaluateDependenciesShouldBeSatisfied(classes, specification));
            }
            return result;
        }

        private void checkNoUpperLayerPackageIsLayer(JavaClasses javaClasses, EvaluationResult result) {
            result.add(
                    all(packages)
                            .that(areDirectRootChildrenOf(this.systemRoot))
                            .should(notBeLayers)
                            .evaluate(javaClasses)
            );
        }

        private void checkCyclicDependenciesBetweenPackages(JavaClasses javaClasses, EvaluationResult result) {
            result.add(
                    slices().matching("(" + this.systemRoot + ".*).(*)..")
                            .should().beFreeOfCycles()
                            .evaluate(javaClasses)
            );
        }

        private void checkAllClassesBelongToFPackagesOrAreDirectGroupChildren(JavaClasses classes, EvaluationResult result) {
            List<String> fPackages = new ArrayList<>();
            for (FPackageDefinition fPackageDefinition : this.fPackageDefinitions) {
                fPackages.add(fPackageDefinition.thePackage);
            }
            result.add(
                classes()
                        .should()
                        .resideInAnyPackage(this.groups.toArray(new String[]{}))
                        .orShould(beInAnyOfPackages(fPackages))
                        .evaluate(classes)
            );
        }

        private void checkEmptyFPackages(JavaClasses classes, EvaluationResult result) {
            for (FPackageDefinition definition : fPackageDefinitions) {
                result.add(evaluateFPackagesShouldNotBeEmpty(classes, definition));
            }
        }

        private void checkDependencyDirectionUp(JavaClasses classes, EvaluationResult result) {
            result.add(
                classes()
                        .should(accessClassesInTheSameOrDirectParentPackageOrUpperLayerOfASiblingPackage)
                        .evaluate(classes)
            );
        }

        private void checkDependencyDirectionDown(JavaClasses classes, EvaluationResult result) {
            result.add(
                classes()
                        .should(accessClassesInTheSameOrDirectSubpackageOrUpperLayerOfASiblingPackage)
                        .evaluate(classes)
            );
        }

        private EvaluationResult evaluateFPackagesShouldNotBeEmpty(JavaClasses classes, FPackageDefinition definition) {
            return classes().that(fPackageDefinitions.containsPredicateFor(definition.getName()))
                    .should(notBeEmptyFor(definition))
                    .allowEmptyShould(false)
                    .evaluate(classes);
        }

        private EvaluationResult evaluateDependenciesShouldBeSatisfied(JavaClasses classes, FPackageDependencySpecification specification) {
            ArchCondition<JavaClass> satisfyFPackageDependenciesCondition = specification.constraint == FPackageDependencyConstraint.ORIGIN
                    ? onlyHaveDependentsWhere(originMatchesIfDependencyIsRelevant(specification.getFPackageName(), specification.allowedFPackages))
                    : onlyHaveDependenciesWhere(targetMatchesIfDependencyIsRelevant(specification.getFPackageName(), specification.allowedFPackages));
            return classes().that(fPackageDefinitions.containsPredicateFor(specification.getFPackageName()))
                    .should(satisfyFPackageDependenciesCondition)
                    .allowEmptyShould(false)
                    .evaluate(classes);
        }

        private DescribedPredicate<Dependency> originMatchesIfDependencyIsRelevant(String ownFPackage, Set<String> allowedAccessors) {
            DescribedPredicate<Dependency> originPackageMatches =
                    dependencyOrigin(fPackageDefinitions.excludeSubpackagePredicateFor(allowedAccessors))
                            .or(dependencyOrigin(fPackageDefinitions.containsPredicateFor(ownFPackage)));

            return ifDependencyIsRelevant(originPackageMatches);
        }

        private DescribedPredicate<Dependency> targetMatchesIfDependencyIsRelevant(String ownFPackage, Set<String> allowedTargets) {
            String thePackage = this.fPackageDefinitions.get(ownFPackage).thePackage;
            String thePackageExcludingSubpackages = thePackage.substring(0, thePackage.length() - 2);
            DescribedPredicate<Dependency> targetPackageMatches =
                    dependencyTarget(fPackageDefinitions.excludeSubpackagePredicateFor(allowedTargets))
                            .or(dependencyTarget(fPackageDefinitions.containsPredicateFor(ownFPackage)))
                            .or(dependencyTarget(areInParentPackageOf(thePackageExcludingSubpackages)));

            return ifDependencyIsRelevant(targetPackageMatches);
        }

        private DescribedPredicate<Dependency> ifDependencyIsRelevant(DescribedPredicate<Dependency> originPackageMatches) {
            return irrelevantDependenciesPredicate.isPresent() ?
                    originPackageMatches.or(irrelevantDependenciesPredicate.get()) :
                    originPackageMatches;
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public void check(JavaClasses classes) {
            Assertions.check(this, classes);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public ArchRule because(String reason) {
            return ArchRule.Factory.withBecause(this, reason);
        }

        @Override
        public ArchRule allowEmptyShould(boolean allowEmptyShould) {
            return inFunctionalArchitecture();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public FunctionalArchitecture as(String newDescription) {
            return new FunctionalArchitecture(
                    fPackageDefinitions,
                    dependencySpecifications,
                    irrelevantDependenciesPredicate,
                    dependencyDirection,
                    groups,
                    systemRoot,
                    fPackagesOn
            );
        }


        @PublicAPI(usage = ACCESS)
        public FunctionalArchitecture ignoreDependency(
                DescribedPredicate<? super JavaClass> origin, DescribedPredicate<? super JavaClass> target) {
            return new FunctionalArchitecture(
                    fPackageDefinitions,
                    dependencySpecifications,
                    irrelevantDependenciesPredicate.add(dependency(origin, target)),
                    dependencyDirection,
                    groups,
                    systemRoot,
                    fPackagesOn
            );
        }

        @PublicAPI(usage = ACCESS)
        public FunctionalArchitecture whereSystemRoot(String systemRoot) {
            return new FunctionalArchitecture(
                    fPackageDefinitions,
                    dependencySpecifications,
                    irrelevantDependenciesPredicate,
                    dependencyDirection,
                    groups,
                    systemRoot,
                    fPackagesOn
            );
        }

        @PublicAPI(usage = ACCESS)
        public FunctionalArchitecture whereFPackagesOn() {
            return new FunctionalArchitecture(
                    fPackageDefinitions,
                    dependencySpecifications,
                    irrelevantDependenciesPredicate,
                    dependencyDirection,
                    groups,
                    systemRoot,
                    true
            );
        }

        @PublicAPI(usage = ACCESS)
        public FunctionalArchitecture whereFPackagesOff() {
            return new FunctionalArchitecture(
                    fPackageDefinitions,
                    dependencySpecifications,
                    irrelevantDependenciesPredicate,
                    dependencyDirection,
                    groups,
                    systemRoot,
                    false
            );
        }

        // Should be called before other constraint declarations (such as whereFPackage())
        @PublicAPI(usage = ACCESS)
        public FunctionalArchitecture whereDependencyDirectionDown() {
            return new FunctionalArchitecture(
                    fPackageDefinitions,
                    dependencySpecifications,
                    irrelevantDependenciesPredicate,
                    DependencyDirection.DOWN,
                    groups,
                    systemRoot,
                    fPackagesOn
            );
        }

        // Should be called before other constraint declarations (such as whereFPackage())
        @PublicAPI(usage = ACCESS)
        public FunctionalArchitecture whereDependencyDirectionUp() {
            return new FunctionalArchitecture(
                    fPackageDefinitions,
                    dependencySpecifications,
                    irrelevantDependenciesPredicate,
                    DependencyDirection.UP,
                    groups,
                    systemRoot,
                    fPackagesOn
            );
        }

        public FunctionalArchitecture group(String thePackage) {
            groups.add(thePackage);
            return this;
        }

        /**
         * Allows restricting access to and from this FPackage. Note that "access" in the context of a FPackage
         * refers to any dependency as defined by {@link Dependency}
         * @param name a FPackage name as specified before via {@link #fPackage(String)}
         * @return a specification to fluently define further restrictions
         */
        @PublicAPI(usage = ACCESS)
        public FPackageDependencySpecification whereFPackage(String name) {
            checkFPackageNamesExist(name);
            return new FPackageDependencySpecification(name);
        }

        private void checkFPackageNamesExist(String... fPackageNames) {
            for (String fPackageName : fPackageNames) {
                checkArgument(
                        fPackageDefinitions.containFPackage(fPackageName),
                        "There is no FPackage name '%s'",
                        fPackageName
                );
            }
        }

        private static ArchCondition<JavaClass> notBeEmptyFor(final FunctionalArchitecture.FPackageDefinition fPackageDefinition) {
            return new FPackageShouldNotBeEmptyCondition(fPackageDefinition);
        }

        private static final class FPackageDefinitions implements Iterable<FPackageDefinition> {
            private final Map<String, FPackageDefinition> fPackageDefinitions = new LinkedHashMap<>();

            void add(FPackageDefinition definition) {
                fPackageDefinitions.put(definition.getName(), definition);
            }

            boolean containFPackage(String fPackageName) {
                return fPackageDefinitions.containsKey(fPackageName);
            }

            DescribedPredicate<JavaClass> containsPredicateFor(String fPackageName) {
                return containsPredicateFor(singleton(fPackageName));
            }

            DescribedPredicate<JavaClass> containsPredicateFor(final Collection<String> fPackageNames) {
                DescribedPredicate<JavaClass> result = alwaysFalse();
                for (FPackageDefinition definition : get(fPackageNames)) {
                    result = result.or(definition.containsPredicate());
                }
                return result;
            }

            DescribedPredicate<JavaClass> excludeSubpackagePredicateFor(String fPackageName) {
                return excludeSubpackagePredicateFor(singleton(fPackageName));
            }

            DescribedPredicate<JavaClass> excludeSubpackagePredicateFor(final Collection<String> fPackageNames) {
                DescribedPredicate<JavaClass> result = alwaysFalse();
                for (FPackageDefinition definition : get(fPackageNames)) {
                    result = result.or(definition.excludeSubpackagePredicate());
                }
                return result;
            }

            FPackageDefinition get(String fPackageName) {
                return fPackageDefinitions.get(fPackageName);
            }

            private Iterable<FPackageDefinition> get(Collection<String> fPackageNames) {
                Set<FPackageDefinition> result = new HashSet<>();
                for (String fPackageName : fPackageNames) {
                    result.add(fPackageDefinitions.get(fPackageName));
                }
                return result;
            }

            @Override
            public Iterator<FPackageDefinition> iterator() {
                return fPackageDefinitions.values().iterator();
            }
        }

        public final class FPackageDefinition extends AbstractFPackageDefinition {

            private FPackageDefinition(String name) {
                super(name);
            }

            /**
             * Defines an FPackage by a predicate, i.e. any {@link JavaClass} that will match the predicate will belong to this FPackage.
             */
            @PublicAPI(usage = ACCESS)
            public FunctionalArchitecture definedBy(
                    String thePackage,
                    DescribedPredicate<? super JavaClass> containsPredicate,
                    DescribedPredicate<? super JavaClass> excludeSubpackagePredicate) {
                checkNotNull(containsPredicate, "Supplied containsPredicate must not be null");
                checkNotNull(containsPredicate, "Supplied excludeSubpackagePredicate must not be null");
                this.thePackage = thePackage;
                this.containsPredicate = containsPredicate.forSubtype();
                this.excludeSubpackagePredicate = excludeSubpackagePredicate.forSubtype();
                FunctionalArchitecture.this.addDependencySpecification(new FPackageDependencySpecification(this.getName()));
                return FunctionalArchitecture.this.addFPackageDefinition(this);
            }
        }

        public final class FPackageDependencySpecification extends AbstractFPackageDependencySpecification {

            FPackageDependencySpecification(String fPackageName) {
                super(fPackageName);
            }

            public FunctionalArchitecture denyFPackageAccess(FPackageDependencyConstraint constraint, String description) {
                allowedFPackages.clear();
                this.constraint = constraint;
                descriptionSuffix = description;
                return FunctionalArchitecture.this.addDependencySpecification(this);
            }

            public FunctionalArchitecture restrictFPackages(FPackageDependencyConstraint constraint, String[] fPackageNames, String descriptionTemplate) {
                checkArgument(fPackageNames.length > 0, "At least 1 FPackage name must be provided.");
                checkFPackageNamesExist(fPackageNames);
                allowedFPackages.addAll(asList(fPackageNames));
                this.constraint = constraint;
                descriptionSuffix = String.format(descriptionTemplate, Joiner.on("', '").join(fPackageNames));
                return FunctionalArchitecture.this.addDependencySpecification(this);
            }
        }
    }

}
