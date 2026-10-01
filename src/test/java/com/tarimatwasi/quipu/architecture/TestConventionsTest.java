package com.tarimatwasi.quipu.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
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

  /** ADR-006: bodyless state-changing requests are defended only by CORS; the test must exist. */
  @ArchTest
  static final ArchRule ADR_006_CONTRACT_TEST =
      methods()
          .that()
          .haveName("bodyless_cross_site_request_is_stopped_by_cors")
          .should()
          .beDeclaredInClassesThat()
          .haveSimpleName("SecurityContractTest")
          .because("ADR-006: a bodyless request relies on CORS rejecting a foreign Origin");

  @ArchTest
  static final ArchRule ADR_006_CONTROLLER_TEST =
      methods()
          .that()
          .haveName("crossSiteBodilessPostIsStoppedByCors")
          .should()
          .beDeclaredInClassesThat()
          .haveSimpleName("AuthBffControllerTest")
          .because("ADR-006: the login endpoint is covered end to end");
}
