#Funkcinė architektūra
<i>archunit-playground</i> repozitorijos dalyje pateikiamas <i>ArchUnit</i> bibliotekos plėtinys - funkcinės paketų strategijos
taisyklių apibrėžimas ir tikrinimas.

##<i>FunctionalArchitecture</i> klasė
Viena pagrindinių šios architektūros sąvokų - <i>FPackage</i> esybė. Ši sąvoka apibrėžia funkcinį paketą.
Architektūros įgyvendinimas leidžia apibrėžti kelis ar daugiau sistemos <i>FPackage</i> bei galimus jų tarpusavio
ryšius. Pagal numatytąją konfigūraciją <i>FPackage</i> negali kreiptis į jokį kitą <i>FPackage</i>, jeigu nenurodyta kitaip.
Šie paketai gali būti grupuojami į grupes. Visos sistemos klasės turi priklausyti kokiam nors <i>FPackage</i>
arba būti aukščiausiame grupės sluoksnyje.

Laukai:
<ul>
    <li><i>fPackageDefinitions</i> - saugo visų <i>FPackage</i> apibrėžimus, į kuriuos įeina:
        <ul>
            <li><i>name</i>: unikalus <i>FPackage</i> vardas</li>
            <li><i>thePackage</i>: <i>FPackage</i> paketo direktorija</li>
            <li><i>containsPredicate</i>: predikatas, nusakantis, kokios klasės priklauso konkrečiam <i>FPackage</i></li>
            <li><i>excludeSubpackagePredicate</i>: predikatas, nusakantis, kokios klasės priklauso
            aukščiausiam <i>FPackage</i> sluoksniui (neįskaitant subpaketų)</li>
        </ul>
    </li>
    <li><i>dependencySpecifications</i> - saugo apibrėžtas galimas priklausomybes tarp <i>FPackage</i>,
    į kurias įeina:
        <ul>
            <li><i>fPackageName</i>: <i>FPackage</i>, kuriam apibrėžta priklausomybė, vardas</li>
            <li><i>allowedFPackages</i>: <i>FPackage</i>, kurie gali būti priklausomybes dalis, vardai</li>
            <li><i>fPackageDependencyConstraint</i>: priklausomybės kryptis (<i>ORIGIN</i> arba <i>TARGET</i>)</li>
            <li><i>descriptionSuffix</i>: priklausomybės aprašymas aiškesniam klaidų išvedimui į konsolę</li>
        </ul>
    </li>
    <li><i>irrelevantDependenciesPredicate</i>: leidžia apibrėžti, su kuriomis klasėmis priklausomybės neturėtų
    būti validuojamos (pavyzdžiui, su visomis klasėmis, kurios nepriklauso sistemai, tokios kaip <i>java.lang.Object</i>)</li>
    <li>dependencyDirection: leidžia apibrėžti, kurios krypties (UP, DOWN, BOTH) priklausomybės tarp paketų leidžiamos
    sistemoje. Numatytoji reikšmė: <i>BOTH</i></li>
    <li><i>groups</i>: leidžia apibrėžti paketus, kurių pagalba grupuojami <i>FPackage</i> (pavyzdžiui <i>domain</i> ir <i>infrastructure</i>)</li>
</ul>

<i>FunctionalArchitecture</i> apibrėžimo ir testavimo pavyzdys:

````
public class FunctionalArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages(SYSTEM_PATH);

    @Test
    public void some_architecture_rule_1() {
        getArchitecture()
                .whereDependencyDirectionUp()
                .whereFPackage("users").mayOnlyAccessFPackages("books")
                .check(classes);
    }

    private CustomArchitectures.FunctionalArchitecture getArchitecture() {
        return functionalArchitecture()
                .ignoreDependency(isInsideThisSystem(SYSTEM_PATH), isOutsideThisSystem(SYSTEM_PATH))
                .fPackage("books").definedBy("com.library.domain.books..")
                .fPackage("events").definedBy("com.library.domain.events..")
                .fPackage("users").definedBy("com.library.domain.users..")
                .fPackage("email").definedBy("com.library.infrastructure.email..")
                .fPackage("pdf").definedBy("com.library.infrastructure.pdf..")
                .whereGroup("com.library.domain")
                .whereGroup("com.library.infrastructure");
    }
}
````

Validuojamos taisyklės:
<ul>
    <li>Ar visi apibrėžti <i>FPackage</i> nepažeidžia nustatytų priklausomybių?</li>
    <li>Ar nėra apibrėžtų tuščių (be jokių klasių) <i>FPackage</i>?</li>
    <li>Ar nėra aukščiausio lygio paketų, kurių pavadinimas (<i>service</i>, <i>controller</i>, <i>persistence</i>) indikuotų, kad jie yra sluoksniniai?</li>
    <li>Ar visos klasės priklauso <i>FPackage</i> arba yra aukščiausiame grupės sluoksnyje?</li>
    <li>Ar priklausomybių kryptis atitinka nustatytą?</li>
</ul>

Bibliotekos praplėtimui testuoti buvo sukurta paprasta paketų struktūra <i>com.library</i>.

Paketų struktūros taisyklės įgyvendintos apibrėžiant abstrakčių <i>ArchUnit</i> bibliotekos
klasių veikimą.

###CustomTransformers
Leidžia apibrėžti <i>JavaClasses</i> transformacija į bet kokias kitas esybes (pavyzdžiui: paketus, domenus).

###CustomPredicates
Leidžia apibrėžti tam tikros esybės atitikimą tam tikroms sąlygoms (pavyzdžiui: ar klasė yra apibrėžtos sistemos viduje).

###CustomConditions
Leidžia apibrėžti sąlygas, kurias turi atitikti esybės.