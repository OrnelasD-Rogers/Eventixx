package com.eventixx.eventcatalog.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

class ArchitectureTest {

  private static JavaClasses importedClasses;

  @BeforeAll
  static void setUp() {
    importedClasses = new ClassFileImporter().importPackages("com.eventixx.eventcatalog");
  }

  @Test
  void noPackageCycles() {
    slices()
        .matching("com.eventixx.eventcatalog.(*)..")
        .should()
        .beFreeOfCycles()
        .check(importedClasses);
  }

  @Test
  void entitiesMustNotDependOnControllersOrServices() {
    noClasses()
        .that()
        .resideInAPackage("..entities..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..controllers..", "..services..")
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
  void dtoMustNotDependOnEntities() {
    noClasses()
        .that()
        .resideInAPackage("..dto..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..entities..")
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
  void transactionalMethodsMustBePublic() {
    methods()
        .that()
        .areAnnotatedWith(Transactional.class)
        .should()
        .bePublic()
        .check(importedClasses);
  }

  @Test
  void controllersMustNotReturnEntityTypes() {
    noClasses()
        .that()
        .resideInAPackage("..controllers..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..entities..")
        .check(importedClasses);
  }

  @Test
  void transactionalMethodsWithCheckedExceptionShouldDeclareRollbackFor() {
    methods()
        .that()
        .areAnnotatedWith(Transactional.class)
        .should(haveRollbackForWhenThrowingCheckedExceptions())
        .check(importedClasses);
  }

  @Test
  void servicesShouldBeAnnotatedWithTransactionalReadOnly() {
    classes()
        .that()
        .areAnnotatedWith(Service.class)
        .should(beAnnotatedWithTransactionalReadOnly())
        .check(importedClasses);
  }

  private static ArchCondition<JavaMethod> haveRollbackForWhenThrowingCheckedExceptions() {
    return new ArchCondition<>("have rollbackFor when throwing checked exceptions") {
      @Override
      public void check(JavaMethod method, ConditionEvents events) {
        boolean hasCheckedException =
            method.getThrowsClause().stream()
                .map(td -> (JavaClass) td.getType())
                .anyMatch(
                    t -> {
                      try {
                        return !RuntimeException.class.isAssignableFrom(t.reflect())
                            && !Error.class.isAssignableFrom(t.reflect());
                      } catch (Exception e) {
                        return false;
                      }
                    });
        if (hasCheckedException) {
          Transactional tx = method.getAnnotationOfType(Transactional.class);
          if (tx.rollbackFor().length == 0) {
            events.add(
                SimpleConditionEvent.violated(
                    method,
                    method.getDescription()
                        + " throws checked exception(s) but @Transactional has no rollbackFor"));
          }
        }
      }
    };
  }

  private static ArchCondition<JavaClass> beAnnotatedWithTransactionalReadOnly() {
    return new ArchCondition<>("be annotated with @Transactional(readOnly = true)") {
      @Override
      public void check(JavaClass clazz, ConditionEvents events) {
        Transactional tx = clazz.getAnnotationOfType(Transactional.class);
        if (tx == null) {
          events.add(
              SimpleConditionEvent.violated(
                  clazz,
                  clazz.getName() + " is a @Service but missing @Transactional(readOnly = true)"));
        } else if (!tx.readOnly()) {
          events.add(
              SimpleConditionEvent.violated(
                  clazz, clazz.getName() + " has @Transactional but readOnly is not true"));
        }
      }
    };
  }

  @Test
  void transactionalMethodsMustNotBeFinal() {
    methods()
        .that()
        .areAnnotatedWith(Transactional.class)
        .should()
        .notBeFinal()
        .check(importedClasses);
  }
}
