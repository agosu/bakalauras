package agosu.bachelor.archunit;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.*;
import com.tngtech.archunit.lang.syntax.PredicateAggregator;
import com.tngtech.archunit.thirdparty.com.google.common.base.Joiner;

import java.util.*;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.*;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
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

    /**
     * Here business component is a top level package (direct root child).
     * TODO: Think about internal business component structure checks
     * (child components and layers)
     * TODO: All the optional business component logic is probably not needed
     * (all components must be not empty, if they are defined)...
     * ... or all components optional?
     */
    public static final class FunctionalArchitecture implements ArchRule {

        private final BusinessComponentDefinitions businessComponentDefinitions;
        private final Set<BusinessComponentDependencySpecification> dependencySpecifications;
        private final PredicateAggregator<Dependency> irrelevantDependenciesPredicate;
        private final Optional<String> overriddenDescription;
        private final boolean optionalBusinessComponents;

        private FunctionalArchitecture() {
            this(
                    new BusinessComponentDefinitions(),
                    new LinkedHashSet<>(),
                    new PredicateAggregator<Dependency>().thatORs(),
                    Optional.empty(),
                    false
            );
        }

        private FunctionalArchitecture(
                BusinessComponentDefinitions businessComponentDefinitions,
                Set<BusinessComponentDependencySpecification> dependencySpecifications,
                PredicateAggregator<Dependency> irrelevantDependenciesPredicate,
                Optional<String> overriddenDescription,
                boolean optionalBusinessComponents) {
            this.businessComponentDefinitions = businessComponentDefinitions;
            this.dependencySpecifications = dependencySpecifications;
            this.irrelevantDependenciesPredicate = irrelevantDependenciesPredicate;
            this.overriddenDescription = overriddenDescription;
            this.optionalBusinessComponents = optionalBusinessComponents;
        }

        /**
         * By default, business components defined with {@link #businessComponent(String)} must not be empty, i.e. contain at least one class.
         * <br>
         * <code>withOptionalBusinessComponents(true)</code> can be used to make all business components optional.<br>
         * <code>withOptionalBusinessComponents(false)</code> still allows to define individual optional business components with {@link #optionalBusinessComponent(String)}.
         * see #optionalBusinessComponent(String)
         */
        @PublicAPI(usage = ACCESS)
        public FunctionalArchitecture withOptionalBusinessComponents(boolean optionalBusinessComponents) {
            return new FunctionalArchitecture(
                    businessComponentDefinitions,
                    dependencySpecifications,
                    irrelevantDependenciesPredicate,
                    overriddenDescription,
                    optionalBusinessComponents
            );
        }

        private FunctionalArchitecture addBusinessComponentDefinition(BusinessComponentDefinition definition) {
            businessComponentDefinitions.add(definition);
            return this;
        }

        private FunctionalArchitecture addDependencySpecification(BusinessComponentDependencySpecification dependencySpecification) {
            dependencySpecifications.add(dependencySpecification);
            return this;
        }

        /**
         * Starts the definition of a new business component within the current {@link #functionalArchitecture() FunctionalArchitecture}.
         * <br>
         * Unless {@link #withOptionalBusinessComponents(boolean) withOptionalBusinessComponents(true)} is used, this layer must not be empty.
         * see #optionalBusinessComponent(String)
         */
        @PublicAPI(usage = ACCESS)
        public BusinessComponentDefinition businessComponent(String name) {
            return new BusinessComponentDefinition(name, false);
        }

        @PublicAPI(usage = ACCESS)
        public BusinessComponentDefinition optionalBusinessComponent(String name) {
            return new BusinessComponentDefinition(name, true);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public String getDescription() {
            if (overriddenDescription.isPresent()) {
                return overriddenDescription.get();
            }

            List<String> lines = newArrayList("Functional architecture consisting of" + (optionalBusinessComponents ? " (optional)" : ""));
            for (BusinessComponentDefinition definition : businessComponentDefinitions) {
                lines.add(definition.toString());
            }
            for (BusinessComponentDependencySpecification specification : dependencySpecifications) {
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
            checkEmptyBusinessComponents(classes, result);
            for (BusinessComponentDependencySpecification specification : dependencySpecifications) {
                result.add(evaluateDependenciesShouldBeSatisfied(classes, specification));
            }
            return result;
        }

        private void checkEmptyBusinessComponents(JavaClasses classes, EvaluationResult result) {
            if (!optionalBusinessComponents) {
                for (BusinessComponentDefinition definition : businessComponentDefinitions) {
                    if (!definition.isOptional()) {
                        result.add(evaluateBusinessComponentsShouldNotBeEmpty(classes, definition));
                    }
                }
            }
        }

        private EvaluationResult evaluateBusinessComponentsShouldNotBeEmpty(JavaClasses classes, BusinessComponentDefinition definition) {
            return classes().that(businessComponentDefinitions.containsPredicateFor(definition.name))
                    .should(notBeEmptyFor(definition))
                    // we need to set `allowEmptyShould(true)` to allow the business component not empty check to be evaluated. This will provide a nicer error message.
                    .allowEmptyShould(true)
                    .evaluate(classes);
        }

        private EvaluationResult evaluateDependenciesShouldBeSatisfied(JavaClasses classes, BusinessComponentDependencySpecification specification) {
            ArchCondition<JavaClass> satisfyBusinessComponentDependenciesCondition = specification.constraint == BusinessComponentDependencyConstraint.ORIGIN
                    ? onlyHaveDependentsWhere(originMatchesIfDependencyIsRelevant(specification.businessComponentName, specification.allowedBusinessComponents))
                    : onlyHaveDependenciesWhere(targetMatchesIfDependencyIsRelevant(specification.businessComponentName, specification.allowedBusinessComponents));
            return classes().that(businessComponentDefinitions.containsPredicateFor(specification.businessComponentName))
                    .should(satisfyBusinessComponentDependenciesCondition)
                    .allowEmptyShould(true)
                    .evaluate(classes);
        }

        private DescribedPredicate<Dependency> originMatchesIfDependencyIsRelevant(String ownBusinessComponent, Set<String> allowedAccessors) {
            DescribedPredicate<Dependency> originPackageMatches =
                    dependencyOrigin(businessComponentDefinitions.containsPredicateFor(allowedAccessors))
                            .or(dependencyOrigin(businessComponentDefinitions.containsPredicateFor(ownBusinessComponent)));

            return ifDependencyIsRelevant(originPackageMatches);
        }

        private DescribedPredicate<Dependency> targetMatchesIfDependencyIsRelevant(String ownBusinessComponent, Set<String> allowedTargets) {
            DescribedPredicate<Dependency> targetPackageMatches =
                    dependencyTarget(businessComponentDefinitions.containsPredicateFor(allowedTargets))
                            .or(dependencyTarget(businessComponentDefinitions.containsPredicateFor(ownBusinessComponent)));

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

        /**
         * This method is equivalent to calling {@link #withOptionalBusinessComponents(boolean)}, which should be preferred in this context
         * as the meaning is easier to understand.
         */
        @Override
        public ArchRule allowEmptyShould(boolean allowEmptyShould) {
            return withOptionalBusinessComponents(allowEmptyShould);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public FunctionalArchitecture as(String newDescription) {
            return new FunctionalArchitecture(
                    businessComponentDefinitions,
                    dependencySpecifications,
                    irrelevantDependenciesPredicate,
                    Optional.of(newDescription),
                    optionalBusinessComponents
            );
        }

        /**
         * Configures the rule to ignore any violation from a specific {@code origin} class to a specific {@code target} class.
         * @param origin A {@link Class} object specifying the origin of a {@link Dependency} to ignore
         * @param target A {@link Class} object specifying the target of a {@link Dependency} to ignore
         * @return a {@link FunctionalArchitecture} to be used as an {@link ArchRule} or further restricted through a fluent API.
         */
        @PublicAPI(usage = ACCESS)
        public FunctionalArchitecture ignoreDependency(Class<?> origin, Class<?> target) {
            return ignoreDependency(equivalentTo(origin), equivalentTo(target));
        }

        /**
         * Same as {@link #ignoreDependency(Class, Class)} but allows specifying origin and target as fully qualified class names.
         */
        @PublicAPI(usage = ACCESS)
        public FunctionalArchitecture ignoreDependency(String originFullyQualifiedClassName, String targetFullyQualifiedClassName) {
            return ignoreDependency(name(originFullyQualifiedClassName), name(targetFullyQualifiedClassName));
        }

        /**
         * Same as {@link #ignoreDependency(Class, Class)} but allows specifying origin and target by freely defined predicates.
         * Any dependency where the {@link Dependency#getOriginClass()} matches the {@code origin} predicate
         * and the {@link Dependency#getTargetClass()} matches the {@code target} predicate will be ignored.
         */
        @PublicAPI(usage = ACCESS)
        public FunctionalArchitecture ignoreDependency(
                DescribedPredicate<? super JavaClass> origin, DescribedPredicate<? super JavaClass> target) {
            return new FunctionalArchitecture(
                    businessComponentDefinitions,
                    dependencySpecifications,
                    irrelevantDependenciesPredicate.add(dependency(origin, target)),
                    overriddenDescription,
                    optionalBusinessComponents
            );
        }

        /**
         * Allows restricting access to and from this business component. Note that "access" in the context of a business component
         * refers to any dependency as defined by {@link Dependency}
         * @param name a business component name as specified befire via {@link #businessComponent(String)}
         * @return a specification to fluently define further restrictions
         */
        @PublicAPI(usage = ACCESS)
        public BusinessComponentDependencySpecification whereBusinessComponent(String name) {
            checkBusinessComponentNamesExist(name);
            return new BusinessComponentDependencySpecification(name);
        }

        private void checkBusinessComponentNamesExist(String... businessComponentNames) {
            for (String businessComponentName : businessComponentNames) {
                checkArgument(
                        businessComponentDefinitions.containBusinessComponent(businessComponentName),
                        "There is no business component name '%s'",
                        businessComponentName
                );
            }
        }

        private static ArchCondition<JavaClass> notBeEmptyFor(final FunctionalArchitecture.BusinessComponentDefinition businessComponentDefinition) {
            return new BusinessComponentShouldNotBeEmptyCondition(businessComponentDefinition);
        }

        private static class BusinessComponentShouldNotBeEmptyCondition extends ArchCondition<JavaClass> {
            private final FunctionalArchitecture.BusinessComponentDefinition businessComponentDefinition;
            private boolean empty = true;

            BusinessComponentShouldNotBeEmptyCondition(final FunctionalArchitecture.BusinessComponentDefinition businessComponentDefinition) {
                super("not be empty");
                this.businessComponentDefinition = businessComponentDefinition;
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
                                    businessComponentDefinition,
                                    String.format("Layer '%s' is empty", businessComponentDefinition.name)
                            )
                    );
                }
            }
        }

        private static final class BusinessComponentDefinitions implements Iterable<BusinessComponentDefinition> {
            private final Map<String, BusinessComponentDefinition> businessComponentDefinitions = new LinkedHashMap<>();

            void add(BusinessComponentDefinition definition) {
                businessComponentDefinitions.put(definition.name, definition);
            }

            boolean containBusinessComponent(String businessComponentName) {
                return businessComponentDefinitions.containsKey(businessComponentName);
            }

            DescribedPredicate<JavaClass> containsPredicateFor(String businessComponentName) {
                return containsPredicateFor(singleton(businessComponentName));
            }

            DescribedPredicate<JavaClass> containsPredicateFor(final Collection<String> businessComponentNames) {
                DescribedPredicate<JavaClass> result = alwaysFalse();
                for (BusinessComponentDefinition definition : get(businessComponentNames)) {
                    result = result.or(definition.containsPredicate());
                }
                return result;
            }

            private Iterable<BusinessComponentDefinition> get(Collection<String> businessComponentNames) {
                Set<BusinessComponentDefinition> result = new HashSet<>();
                for (String businessComponentName : businessComponentNames) {
                    result.add(businessComponentDefinitions.get(businessComponentName));
                }
                return result;
            }

            @Override
            public Iterator<BusinessComponentDefinition> iterator() {
                return businessComponentDefinitions.values().iterator();
            }
        }

        public final class BusinessComponentDefinition {
            private final String name;
            private final boolean optional;
            private DescribedPredicate<JavaClass> containsPredicate;

            private BusinessComponentDefinition(String name, boolean optional) {
                checkState(!isNullOrEmpty(name), "Business component name must be present");
                this.name = name;
                this.optional = optional;
            }

            /**
             * Defines a business component by a predicate, i.e. any {@link JavaClass} that will match the predicate will belong to this business component.
             */
            @PublicAPI(usage = ACCESS)
            public FunctionalArchitecture definedBy(DescribedPredicate<? super JavaClass> predicate) {
                checkNotNull(predicate, "Supplied predicate must not be null");
                this.containsPredicate = predicate.forSubtype();
                return FunctionalArchitecture.this.addBusinessComponentDefinition(this);
            }

            @PublicAPI(usage = ACCESS)
            public FunctionalArchitecture definedBy(String... packageIdentifiers) {
                String description = String.format("'%s", Joiner.on(", ").join(packageIdentifiers));
                return definedBy(resideInAnyPackage(packageIdentifiers).as(description));
            }

            boolean isOptional() {
                return optional;
            }

            DescribedPredicate<JavaClass> containsPredicate() {
                return containsPredicate;
            }

            @Override
            public String toString() {
                return String.format("%s business component '%s' (%s)", optional ? "optional " : "", name, containsPredicate);
            }
        }

        private enum BusinessComponentDependencyConstraint {
            ORIGIN,
            TARGET
        }

        public final class BusinessComponentDependencySpecification {
            private final String businessComponentName;
            private final Set<String> allowedBusinessComponents = new LinkedHashSet<>();
            private BusinessComponentDependencyConstraint constraint;
            private String descriptionSuffix;

            private BusinessComponentDependencySpecification(String businessComponentName) {
                this.businessComponentName = businessComponentName;
            }

            /**
             * Forbids any {@link Dependency dependency} from another business component to this business component.
             * @return a {@link FunctionalArchitecture} to be used as an {@link ArchRule} or further restricted through a fluent API.
             */
            @PublicAPI(usage = ACCESS)
            public FunctionalArchitecture mayNotBeAccessedByAnyBusinessComponent() {
                return denyBusinessComponentAccess(BusinessComponentDependencyConstraint.ORIGIN, "may not be accessed by any business component");
            }

            /**
             * Restricts this business component to only allow the specified business components to have {@link Dependency dependencies} to this business component.
             * @param businessComponentNames the names of other business components (as specified by {@link #businessComponent(String)}) that may access this business component
             * @return a {@link FunctionalArchitecture} to be used as an {@link ArchRule} or further restricted through a fluent API.
             */
            @PublicAPI(usage = ACCESS)
            public FunctionalArchitecture mayOnlyBeAccessedByBusinessComponents(String... businessComponentNames) {
                return restrictBusinessComponents(BusinessComponentDependencyConstraint.ORIGIN, businessComponentNames, "may only be accessed by layers ['%s']");
            }

            /**
             * Forbids any {@link Dependency dependency} from this business component to any other business component.
             * @return a {@link FunctionalArchitecture} to be used as an {@link ArchRule} or further restricted through a fluent API.
             */
            @PublicAPI(usage = ACCESS)
            public FunctionalArchitecture mayNotAccessAnyBusinessComponent() {
                return denyBusinessComponentAccess(BusinessComponentDependencyConstraint.TARGET, "may not access any business component");
            }

            /**
             * Restricts this business component to only allow {@link Dependency dependencies} to the specified business components.
             * @param businessComponentNames the only names of other business components (as specified by {@link #businessComponent(String)}) that this business component may access
             * @return a {@link FunctionalArchitecture} to be used as an {@link ArchRule} or further restricted through a fluent API.
             */
            @PublicAPI(usage = ACCESS)
            public FunctionalArchitecture mayOnlyAccessBusinessComponents(String... businessComponentNames) {
                return restrictBusinessComponents(BusinessComponentDependencyConstraint.TARGET, businessComponentNames, "may only access business components ['%s']");
            }

            private FunctionalArchitecture denyBusinessComponentAccess(BusinessComponentDependencyConstraint constraint, String description) {
                allowedBusinessComponents.clear();
                this.constraint = constraint;
                descriptionSuffix = description;
                return FunctionalArchitecture.this.addDependencySpecification(this);
            }

            private FunctionalArchitecture restrictBusinessComponents(BusinessComponentDependencyConstraint constraint, String[] businessComponentNames, String descriptionTemplate) {
                checkArgument(businessComponentNames.length > 0, "At least 1 business component name must be provided.");
                checkBusinessComponentNamesExist(businessComponentNames);
                allowedBusinessComponents.addAll(asList(businessComponentNames));
                this.constraint = constraint;
                descriptionSuffix = String.format(descriptionTemplate, Joiner.on("', '").join(businessComponentNames));
                return FunctionalArchitecture.this.addDependencySpecification(this);
            }

            @Override
            public String toString() {
                return String.format("where business component '%s' %s", businessComponentName, descriptionSuffix);
            }
        }
    }

}
