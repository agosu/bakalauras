package agosu.bachelor.archunit;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.ArrayList;
import java.util.List;

import static agosu.bachelor.archunit.Utils.*;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependenciesWhere;
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
                    if (javaClass.getPackageName().contains(getPackageExcludingSubpackages(thePackage))) {
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
                String packageName = javaClass.getPackageName();
                boolean isThereItShouldBe = packageName.equals(thePackage) ||
                        packageName.equals(getParentPackage(thePackage)) ||
                        packageName.matches(getSubpackageRegex(thePackage));
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

    public static ArchCondition<JavaClass> notAccessClassesInSubpackages() {
        return new ArchCondition<JavaClass>("not access classes in subpackages") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                List<Dependency> dependenciesThatAreNotAllowed = clazz.getDirectDependenciesFromSelf().stream()
                        .filter(it -> it.getTargetClass().getPackageName().matches(getSubpackageRegex(clazz.getPackageName())))
                        .collect(toList());

                if (!dependenciesThatAreNotAllowed.isEmpty()) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    clazz,
                                    format(
                                            "Class %s has dependencies in subpackages",
                                            clazz.getName()
                                    )
                            )
                    );
                }
            }
        };
    }

    public static ArchCondition<JavaClass> notAccessClassesInAncestorPackages(){
        return new ArchCondition<JavaClass>("not access classes in ancestor packages") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                List<Dependency> dependenciesThatAreNotAllowed = clazz.getDirectDependenciesFromSelf().stream()
                        .filter(it -> {
                            int numberOfPackageLayers = clazz.getPackageName().split("[.]").length;
                            List<String> ancestors = new ArrayList<>();
                            ancestors.add(getParentPackage(clazz.getPackageName()));
                            for (int i = 0; i < numberOfPackageLayers - 2; i++) {
                                ancestors.add(getParentPackage(ancestors.get(i)));
                            }
                            String targetPackage = it.getTargetClass().getPackageName();
                            boolean result = false;
                            for (String ancestor : ancestors) {
                                result = result || targetPackage.equals(ancestor);
                            }
                            return result;
                        })
                        .collect(toList());

                if (!dependenciesThatAreNotAllowed.isEmpty()) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    clazz,
                                    format(
                                            "Class %s has dependencies in ancestor packages",
                                            clazz.getName()
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
                        .filter(it -> {
                            String targetPackage = it.getTargetClass().getPackageName();
                            return !(targetPackage.equals(clazz.getPackageName())) &&
                                    targetPackage.contains(systemRoot);
                        })
                        .collect(toList());

                if (!dependenciesThatResideNotInTheSamePackage.isEmpty()) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    clazz,
                                    format(
                                            "Class %s has dependencies outside of it's package",
                                            clazz.getName()
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
                        .filter(it -> {
                            String targetPackage = it.getTargetClass().getPackageName();
                            return !(targetPackage.equals(getParentPackage(clazz.getPackageName()))) &&
                                    targetPackage.contains(systemRoot);
                        })
                        .collect(toList());

                if (!dependenciesThatResideNotInDirectParentPackage.isEmpty()) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    clazz,
                                    format(
                                            "Class %s has dependencies not in it's direct parent package",
                                            clazz.getName()
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
                        .filter(it -> {
                            String targetPackage = it.getTargetClass().getPackageName();
                            return !(targetPackage.matches(getSubpackageRegex(clazz.getPackageName()))) &&
                                    targetPackage.contains(systemRoot);
                        })
                        .collect(toList());

                if (!dependenciesThatResideNotInDirectSubpackage.isEmpty()) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    clazz,
                                    format(
                                            "Class %s has dependencies not in it's direct subpackage",
                                            clazz.getName()
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
                        .filter(it -> {
                            String targetPackage = it.getTargetClass().getPackageName();
                            return !(targetPackage.matches(getSiblingPackageOrSelfRegex(clazz.getPackageName()))) &&
                                    targetPackage.contains(systemRoot);
                        })
                        .collect(toList());

                if (!dependenciesThatResideNotInUpperLayerOfASiblingPackage.isEmpty()) {
                    events.add(
                            SimpleConditionEvent.violated(
                                    clazz,
                                    format(
                                            "Class %s has dependencies not in upper layer of a sibling package",
                                            clazz.getName()
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

    private static boolean belongsToFPackage(List<String> fPackages, String targetPackage) {
        for (String fPackage : fPackages) {
            if (targetPackage.equals(fPackage)) {
                return true;
            }
        }

        return false;
    }

}
