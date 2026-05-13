package com.eventixx.eventcatalog.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .importPackages("com.eventixx.eventcatalog");
    }

    @Test
    void noPackageCycles() {
        slices()
                .matching("com.eventixx.eventcatalog.(*)..")
                .should().beFreeOfCycles()
                .check(importedClasses);
    }

    @Test
    void entitiesMustNotDependOnControllersOrServices() {
        noClasses()
                .that().resideInAPackage("..entities..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..controllers..", "..services..")
                .check(importedClasses);
    }

    @Test
    void repositoriesMustNotDependOnControllers() {
        noClasses()
                .that().resideInAPackage("..repositories..")
                .should().dependOnClassesThat()
                .resideInAPackage("..controllers..")
                .check(importedClasses);
    }

    @Test
    void dtoMustNotDependOnEntities() {
        noClasses()
                .that().resideInAPackage("..dto..")
                .should().dependOnClassesThat()
                .resideInAPackage("..entities..")
                .check(importedClasses);
    }

    @Test
    void restControllersShouldHaveNameEndingWithController() {
        classes()
                .that().areAnnotatedWith(RestController.class)
                .should().haveSimpleNameEndingWith("Controller")
                .check(importedClasses);
    }

    @Test
    void servicesShouldHaveNameEndingWithService() {
        classes()
                .that().areAnnotatedWith(Service.class)
                .should().haveSimpleNameEndingWith("Service")
                .check(importedClasses);
    }

    @Test
    void noSystemOutPrintln() {
        noClasses()
                .should().callMethod(System.class, "out")
                .check(importedClasses);
    }

    @Test
    void transactionalMethodsMustBePublic() {
        methods()
                .that().areAnnotatedWith(Transactional.class)
                .should().bePublic()
                .check(importedClasses);
    }

    @Test
    void transactionalMethodsMustNotBeFinal() {
        methods()
                .that().areAnnotatedWith(Transactional.class)
                .should().notBeFinal()
                .check(importedClasses);
    }
}
