package com.eventixx.__FLAT_PACKAGE__.arch;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    @Test
    void entities_shouldNotDependOn_controllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..entities..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..controllers..", "..services..", "..dto..");
        rule.check(new ClassFileImporter()
                .importPackages("com.eventixx.__FLAT_PACKAGE__"));
    }

    @Test
    void repositories_shouldNotDependOn_controllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..repositories..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..controllers..");
        rule.check(new ClassFileImporter()
                .importPackages("com.eventixx.__FLAT_PACKAGE__"));
    }

    @Test
    void dto_shouldNotDependOn_entities() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..dto..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..entities..");
        rule.check(new ClassFileImporter()
                .importPackages("com.eventixx.__FLAT_PACKAGE__"));
    }

    @Test
    void noCyclicDependencies() {
        ArchRule rule = classes()
                .should().onlyHaveDependenciesOutsidePackage("..dto..")
                .andShould().onlyHaveDependenciesOutsidePackage("..repositories..");
        rule.check(new ClassFileImporter()
                .importPackages("com.eventixx.__FLAT_PACKAGE__"));
    }
}
