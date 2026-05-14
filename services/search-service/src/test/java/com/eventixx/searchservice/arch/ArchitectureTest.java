package com.eventixx.searchservice.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

class ArchitectureTest {

  private static JavaClasses importedClasses;

  @BeforeAll
  static void setUp() {
    importedClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.eventixx.searchservice");
  }

  @Test
  void noPackageCycles() {
    slices()
        .matching("com.eventixx.searchservice.(*)..")
        .should()
        .beFreeOfCycles()
        .check(importedClasses);
  }

  @Test
  void repositoriesMustNotDependOnControllersOrServices() {
    noClasses()
        .that()
        .resideInAPackage("..repositories..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..controllers..", "..services..", "..dto..", "..exceptions..")
        .check(importedClasses);
  }

  @Test
  void repositoriesMustNotDependOnControllers() {
    noClasses()
        .that()
        .resideInAPackage("..repositories..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..controllers..")
        .check(importedClasses);
  }

  @Test
  void restControllersShouldHaveNameEndingWithController() {
    classes()
        .that()
        .areAnnotatedWith(RestController.class)
        .should()
        .haveSimpleNameEndingWith("Controller")
        .check(importedClasses);
  }

  @Test
  void servicesShouldHaveNameEndingWithService() {
    classes()
        .that()
        .areAnnotatedWith(Service.class)
        .should()
        .haveSimpleNameEndingWith("Service")
        .check(importedClasses);
  }

  @Test
  void noSystemOutPrintln() {
    noClasses().should().callMethod(System.class, "out").check(importedClasses);
  }

  @Test
  void layeredArchitectureRespected() {
    layeredArchitecture()
        .consideringAllDependencies()
        .layer("Controllers")
        .definedBy("..controllers..")
        .layer("Services")
        .definedBy("..services..")
        .layer("Repositories")
        .definedBy("..repositories..")
        .whereLayer("Controllers")
        .mayNotBeAccessedByAnyLayer()
        .whereLayer("Services")
        .mayOnlyBeAccessedByLayers("Controllers")
        .whereLayer("Repositories")
        .mayOnlyBeAccessedByLayers("Services")
        .check(importedClasses);
  }

  @Test
  void dtoMustNotDependOnRepositories() {
    noClasses()
        .that()
        .resideInAPackage("..dto..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..repositories..")
        .check(importedClasses);
  }
}
