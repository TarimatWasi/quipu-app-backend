package com.tarimatwasi.quipu.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Set;

/** Conventions for test code (BE-SPR-TST-01, TST-03, TST-05, TST-07). */
@AnalyzeClasses(packages = ArchitectureRules.ROOT)
class TestConventionsTest {

  @ArchTest
  static final ArchRule BE_SPR_TST_01 =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.junit", "org.junit.runner..", "org.hamcrest..");

  @ArchTest
  static final ArchRule BE_SPR_TST_03 =
      noClasses().should().dependOnClassesThat().resideInAnyPackage("org.h2..");

  /** The guide tests (TST-08) live in these packages and are not tests of a single class. */
  @ArchTest
  static final ArchRule BE_SPR_TST_05 =
      ArchitectureConditions.testsLiveWithTheirClass(Set.of(".architecture", ".contract", ".db"));

  @ArchTest
  static final ArchRule BE_SPR_TST_07 =
      noClasses().should().callMethod(Thread.class, "sleep", long.class);
}
