package agosu.bachelor.archunit;

public class Utils {

    public static String getSubpackageRegex(String thePackage) {
        return thePackage + "[.][^.]+";
    }

    public static String getParentPackage(String thePackage) {
        return thePackage.substring(0, thePackage.lastIndexOf('.'));
    }

    public static String getSiblingPackageOrSelfRegex(String thePackage) {
        return getSubpackageRegex(getParentPackage(thePackage));
    }

    public static String getPackageExcludingSubpackages(String thePackage) {
        return thePackage.substring(0, thePackage.length() - 2);
    }

}
