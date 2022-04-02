package agosu.bachelor.archunit;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.*;
import com.tngtech.archunit.thirdparty.com.google.common.base.Joiner;

import java.util.*;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.*;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependenciesWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependentsWhere;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.thirdparty.com.google.common.base.Preconditions.*;
import static com.tngtech.archunit.thirdparty.com.google.common.base.Strings.isNullOrEmpty;
import static com.tngtech.archunit.thirdparty.com.google.common.collect.Lists.newArrayList;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

public final class CustomArchitectures {

    private CustomArchitectures() {}

    @PublicAPI(usage = ACCESS)
    public static FunctionalArchitecture functionalArchitecture() {
        return new FunctionalArchitecture();
    }

    public static final class FunctionalArchitecture implements ArchRule {

        private final FPackageDefinitions fPackageDefinitions;
        private final Set<FPackageDependencySpecification> dependencySpecifications;

        private FunctionalArchitecture() {
            this(
                    new FPackageDefinitions(),
                    new LinkedHashSet<>()
            );
        }

        private FunctionalArchitecture(
                FPackageDefinitions fPackageDefinitions,
                Set<FPackageDependencySpecification> dependencySpecifications) {
            this.fPackageDefinitions = fPackageDefinitions;
            this.dependencySpecifications = dependencySpecifications;
        }

        @PublicAPI(usage = ACCESS)
        public FunctionalArchitecture inFunctionalArchitecture() {
            return new FunctionalArchitecture(
                    fPackageDefinitions,
                    dependencySpecifications
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
            for (FPackageDependencySpecification specification : dependencySpecifications) {
                result.add(evaluateDependenciesShouldBeSatisfied(classes, specification));
            }
            return result;
        }

        private void checkEmptyFPackages(JavaClasses classes, EvaluationResult result) {
            for (FPackageDefinition definition : fPackageDefinitions) {
                result.add(evaluateFPackagesShouldNotBeEmpty(classes, definition));
            }
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
                    dependencyOrigin(fPackageDefinitions.containsPredicateFor(allowedAccessors))
                            .or(dependencyOrigin(fPackageDefinitions.containsPredicateFor(ownFPackage)));

            return ifDependencyIsRelevant(originPackageMatches);
        }

        private DescribedPredicate<Dependency> targetMatchesIfDependencyIsRelevant(String ownFPackage, Set<String> allowedTargets) {
            DescribedPredicate<Dependency> targetPackageMatches =
                    dependencyTarget(fPackageDefinitions.containsPredicateFor(allowedTargets))
                            .or(dependencyTarget(fPackageDefinitions.containsPredicateFor(ownFPackage)));

            return ifDependencyIsRelevant(targetPackageMatches);
        }

        private DescribedPredicate<Dependency> ifDependencyIsRelevant(DescribedPredicate<Dependency> originPackageMatches) {
            return originPackageMatches;
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
                    dependencySpecifications
            );
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
            public FunctionalArchitecture definedBy(DescribedPredicate<? super JavaClass> predicate) {
                checkNotNull(predicate, "Supplied predicate must not be null");
                this.containsPredicate = predicate.forSubtype();
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
