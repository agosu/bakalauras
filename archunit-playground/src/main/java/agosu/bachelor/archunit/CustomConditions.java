package agosu.bachelor.archunit;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.List;

import static agosu.bachelor.archunit.Utils.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class CustomConditions {

    public static final ArchCondition<JavaPackage> notBeLayers = new ArchCondition<JavaPackage>("not be layers") {
        @Override
        public void check(JavaPackage javaPackage, ConditionEvents events) {
            if (javaPackage.getRelativeName().matches("service|controller|persistence")) {
                events.add(SimpleConditionEvent.violated(javaPackage, format("Package %s violates no layers rule", javaPackage.getName())));
            }
        }
    };

    public static ArchCondition<JavaClass> beInAnyOfPackages(List<String> packages) {
        return new ArchCondition<JavaClass>("be in any given package") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean okay = false;
                for (String thePackage : packages) {
                    if (javaClass.getPackageName().contains(thePackage.substring(0, thePackage.length() - 2))) {
                        okay = true;
                    }
                }
                if (!okay) {
                    // message is simplified for example purposes
                    events.add(SimpleConditionEvent.violated(javaClass, "is not in any FPackage"));
                }
            }
        };
    }

    public static ArchCondition<JavaClass> beInTheSameOrParentPackageOrSubpackage(String thePackage) {
        return new ArchCondition<JavaClass>("be in the same or parent package or subpackage") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean isThereItShouldBe = javaClass.getPackageName().equals(thePackage)
                        || javaClass.getPackageName().equals(getParentPackage(thePackage))
                        || javaClass.getPackageName().matches(getSubpackageRegex(thePackage));
                if (!isThereItShouldBe) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    javaClass,
                                    format(
                                            "Class %s is not in the %s package or parent package or subpackage",
                                            javaClass.getName(),
                                            thePackage
                                    )
                            )
                    );
                }
            }
        };
    }

    public static ArchCondition<JavaClass> accessClassesInTheSameOrDirectParentPackageOrUpperLayerOfASiblingPackage(String systemRoot, boolean groups) {
        return new ArchCondition<JavaClass>("have dependencies those direction is up") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                List<Dependency> dependenciesThatAreNotAllowed = clazz.getDirectDependenciesFromSelf().stream()
                        .filter(it ->
                                !(clazz.getPackageName().equals(it.getTargetClass().getPackageName())) &&
                                        !(it.getTargetClass().getPackageName().equals(getParentPackage(clazz.getPackageName()))) &&
                                        !(it.getTargetClass().getPackageName().matches(getSiblingPackageOrSelfRegex(clazz.getPackageName()))) &&
                                        !belongsToGroup(groups, it.getTargetClass().getPackageName(), systemRoot) &&
                                        it.getTargetClass().getPackageName().contains(systemRoot))
                        .collect(toList());

                if (!dependenciesThatAreNotAllowed.isEmpty()) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    clazz,
                                    format(
                                            "Class %s has dependencies those direction is down",
                                            clazz.getSimpleName()
                                    )
                            )
                    );
                }
            }
        };
    }

    public static ArchCondition<JavaClass> accessClassesInTheSameOrDirectSubpackageOrUpperLayerOfASiblingPackage(String systemRoot, boolean groups){
        return new ArchCondition<JavaClass>("have dependencies those direction is down") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                List<Dependency> dependenciesThatAreNotAllowed = clazz.getDirectDependenciesFromSelf().stream()
                        .filter(it ->
                                !(clazz.getPackageName().equals(it.getTargetClass().getPackageName())) &&
                                        !(it.getTargetClass().getPackageName().matches(getSubpackageRegex(clazz.getPackageName()))) &&
                                        !(it.getTargetClass().getPackageName().matches(getSiblingPackageOrSelfRegex(clazz.getPackageName()))) &&
                                        !belongsToGroup(groups, it.getTargetClass().getPackageName(), systemRoot) &&
                                        it.getTargetClass().getPackageName().contains(systemRoot))
                        .collect(toList());

                if (!dependenciesThatAreNotAllowed.isEmpty()) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    clazz,
                                    format(
                                            "Class %s has dependencies those direction is up",
                                            clazz.getSimpleName()
                                    )
                            )
                    );
                }
            }
        };
    }

    public static ArchCondition<JavaClass> accessClassesInTheSamePackage(String systemRoot){
        return new ArchCondition<JavaClass>("access classes in the same package") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                List<Dependency> dependenciesThatResideNotInTheSamePackage = clazz.getDirectDependenciesFromSelf().stream()
                        .filter(it ->
                                !(clazz.getPackageName().equals(it.getTargetClass().getPackageName())) &&
                                        it.getTargetClass().getPackageName().contains(systemRoot))
                        .collect(toList());

                if (!dependenciesThatResideNotInTheSamePackage.isEmpty()) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    clazz,
                                    format(
                                            "Class %s has dependencies outside of it's package",
                                            clazz.getSimpleName()
                                    )
                            )
                    );
                }
            }
        };
    }

    public static ArchCondition<JavaClass> accessClassesInDirectParentPackage(String systemRoot){
        return new ArchCondition<JavaClass>("access classes in direct parent package") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                List<Dependency> dependenciesThatResideNotInDirectParentPackage = clazz.getDirectDependenciesFromSelf().stream()
                        .filter(it -> !(it.getTargetClass().getPackageName().equals(getParentPackage(clazz.getPackageName())))
                                && it.getTargetClass().getPackageName().contains(systemRoot))
                        .collect(toList());

                if (!dependenciesThatResideNotInDirectParentPackage.isEmpty()) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    clazz,
                                    format(
                                            "Class %s has dependencies not in it's direct parent package",
                                            clazz.getSimpleName()
                                    )
                            )
                    );
                }
            }
        };
    }

    public static ArchCondition<JavaClass> accessClassesInDirectSubpackage(String systemRoot){
        return new ArchCondition<JavaClass>("access classes in direct subpackage") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                List<Dependency> dependenciesThatResideNotInDirectSubpackage = clazz.getDirectDependenciesFromSelf().stream()
                        .filter(it -> !(it.getTargetClass().getPackageName().matches(getSubpackageRegex(clazz.getPackageName())))
                                && it.getTargetClass().getPackageName().contains(systemRoot))
                        .collect(toList());

                if (!dependenciesThatResideNotInDirectSubpackage.isEmpty()) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    clazz,
                                    format(
                                            "Class %s has dependencies not in it's direct subpackage",
                                            clazz.getSimpleName()
                                    )
                            )
                    );
                }
            }
        };
    }

    public static ArchCondition<JavaClass> accessClassesInUpperLayerOfASiblingPackage(String systemRoot){
        return new ArchCondition<JavaClass>("access classes in upper layer of a sibling package") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                List<Dependency> dependenciesThatResideNotInUpperLayerOfASiblingPackage = clazz.getDirectDependenciesFromSelf().stream()
                        .filter(it -> !(it.getTargetClass().getPackageName().matches(getSiblingPackageOrSelfRegex(clazz.getPackageName())))
                                && it.getTargetClass().getPackageName().contains(systemRoot))
                        .collect(toList());

                if (!dependenciesThatResideNotInUpperLayerOfASiblingPackage.isEmpty()) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    clazz,
                                    format(
                                            "Class %s has dependencies not in upper layer of a sibling package",
                                            clazz.getSimpleName()
                                    )
                            )
                    );
                }
            }
        };
    }

    private static boolean belongsToGroup(boolean groups, String thePackage, String systemRoot) {
        return groups && thePackage.matches(getSubpackageRegex(systemRoot));
    }

}
