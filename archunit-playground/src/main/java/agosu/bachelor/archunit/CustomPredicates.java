package agosu.bachelor.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;

import static agosu.bachelor.archunit.Utils.getParentPackage;
import static java.lang.String.format;

public class CustomPredicates {

    public static DescribedPredicate<JavaPackage> areDirectRootChildrenOf(String root) {
        return new DescribedPredicate<JavaPackage>(format("are direct root <%s> children", root)) {
            @Override
            public boolean apply(JavaPackage javaPackage) {
                String packageNameMinusRootName = javaPackage.getName().replace(root + ".", "");
                return !packageNameMinusRootName.isEmpty() && !packageNameMinusRootName.contains(".");
            }
        };
    }

    public static DescribedPredicate<JavaClass> isOutsideThisSystem(String systemRootPackage) {
        return new DescribedPredicate<JavaClass>("is outside this system: " + systemRootPackage) {
            @Override
            public boolean apply(JavaClass javaClass) {
                return !javaClass.getPackageName().contains(systemRootPackage);
            }
        };
    }

    public static DescribedPredicate<JavaClass> isInsideThisSystem(String systemRootPackage) {
        return new DescribedPredicate<JavaClass>("is inside this system: " + systemRootPackage) {
            @Override
            public boolean apply(JavaClass javaClass) {
                return javaClass.getPackageName().contains(systemRootPackage);
            }
        };
    }

    public static DescribedPredicate<JavaClass> areInTheSameOrParentPackageOrSubpackage(String thePackage) {
        String theParentPackage = thePackage.substring(0, thePackage.lastIndexOf('.') - 1);
        String theSubpackage = thePackage + "[.][^.]+";
        return new DescribedPredicate<JavaClass>("are in the same or parent package or subpackage") {
            @Override
            public boolean apply(JavaClass javaClass) {
                return javaClass.getPackageName().equals(thePackage)
                        || javaClass.getPackageName().equals(theParentPackage)
                        || javaClass.getPackageName().matches(theSubpackage);
            }
        };
    }

    public  static DescribedPredicate<JavaClass> areInParentPackageOf(String thePackage) {
        return new DescribedPredicate<JavaClass>(format("are in parent package of %s", thePackage)) {
            @Override
            public boolean apply(JavaClass javaClass) {
                return javaClass.getPackageName().equals(getParentPackage(thePackage));
            }
        };
    }

}
