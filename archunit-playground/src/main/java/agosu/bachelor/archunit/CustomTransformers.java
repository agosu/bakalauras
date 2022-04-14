package agosu.bachelor.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.lang.AbstractClassesTransformer;
import com.tngtech.archunit.lang.ClassesTransformer;

import java.util.HashSet;
import java.util.Set;

public class CustomTransformers {

    public static final ClassesTransformer<JavaPackage> packages = new AbstractClassesTransformer<JavaPackage>("packages") {
        @Override
        public Iterable<JavaPackage> doTransform(JavaClasses classes) {
            Set<JavaPackage> result = new HashSet<>();
            classes.getDefaultPackage().accept(DescribedPredicate.alwaysTrue(), new JavaPackage.PackageVisitor() {
                @Override
                public void visit(JavaPackage javaPackage) {
                    result.add(javaPackage);
                }
            });
            return result;
        }
    };

}
