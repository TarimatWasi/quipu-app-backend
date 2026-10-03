package com.tarimatwasi.quipu.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tarimatwasi.fixtures.Fixtures;
import com.tarimatwasi.fixtures.architecture.BadTest;
import com.tarimatwasi.fixtures.architecture.Good;
import com.tarimatwasi.fixtures.architecture.GoodTest;
import com.tarimatwasi.fixtures.bff.adapter.in.rest.BffRoutes;
import com.tarimatwasi.fixtures.core.adapter.in.rest.CoreRoutes;
import com.tarimatwasi.fixtures.demo.adapter.out.AsyncInAdapter;
import com.tarimatwasi.fixtures.demo.application.AsyncClassInService;
import com.tarimatwasi.fixtures.demo.application.AsyncInService;
import com.tarimatwasi.fixtures.demo.application.EnableAsyncInService;
import com.tarimatwasi.fixtures.demo.application.SchedulingEnabled;
import com.tarimatwasi.fixtures.demo.application.Service;
import com.tarimatwasi.fixtures.demo.domain.BadDomain;
import com.tarimatwasi.fixtures.demo.domain.GoodDomain;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The custom conditions must fail on a violation and pass on a compliant class. */
class ArchitectureConditionsTest {

  private static JavaClasses classes(Class<?>... types) {
    return new ClassFileImporter().importClasses(types);
  }

  private static void assertFails(ArchRule rule, Class<?>... types) {
    assertThatThrownBy(() -> rule.check(classes(types))).isInstanceOf(AssertionError.class);
  }

  private static void assertPasses(ArchRule rule, Class<?>... types) {
    assertThatCode(() -> rule.check(classes(types))).doesNotThrowAnyException();
  }

  @Test
  void cfg03_allows_one_value_and_rejects_two() {
    var rule = ArchitectureConditions.atMostOneValuePerClass();
    assertFails(rule, Fixtures.TwoValues.class);
    assertPasses(rule, Fixtures.OneValue.class);
  }

  @Test
  void cfg04_rejects_value_on_fields_and_spel() {
    var rule = ArchitectureConditions.noValueOnFieldsNorSpel();
    assertFails(rule, Fixtures.FieldValue.class);
    assertFails(rule, Fixtures.SpelValue.class);
    assertPasses(rule, Fixtures.OneValue.class);
  }

  @Test
  void web03_requires_valid_on_request_body() {
    var rule = ArchitectureConditions.requestBodyIsValidated();
    assertFails(rule, Fixtures.UnvalidatedBody.class);
    assertPasses(rule, Fixtures.ValidatedBody.class);
  }

  @Test
  void dat07_requires_lazy_to_one_relations() {
    var rule = ArchitectureConditions.toOneRelationsAreLazy();
    assertFails(rule, Fixtures.EagerRelation.class);
    assertPasses(rule, Fixtures.LazyRelation.class);
  }

  @Test
  void dat07_inspectsGettersOfPropertyAccessEntities() {
    var rule = ArchitectureConditions.toOneRelationsAreLazy();
    assertFails(rule, Fixtures.EagerGetter.class);
    assertPasses(rule, Fixtures.LazyGetter.class);
  }

  @Test
  void con01_rejectsEveryThreadConstructor() {
    var rule = ArchitectureRules.BE_SPR_CON_01;
    assertFails(rule, Fixtures.ThreadWithName.class);
    assertFails(rule, Fixtures.ThreadWithTaskAndName.class);
    assertFails(rule, Fixtures.ThreadSubclassInstance.class);
    assertPasses(rule, Fixtures.ManagedThreadFree.class);
  }

  @Test
  void con01_allowsAsyncOnlyInTheOutputAdapters() {
    var rule = ArchitectureRules.BE_SPR_CON_01;
    assertFails(rule, AsyncInService.class);
    assertFails(rule, EnableAsyncInService.class);
    assertFails(rule, AsyncClassInService.class);
    assertPasses(rule, AsyncInAdapter.class);
  }

  @Test
  void con01_stillRequiresAnAdrForScheduledTasks() {
    assertFails(ArchitectureRules.BE_SPR_CON_01, SchedulingEnabled.class);
  }

  @Test
  void cfg01_requires_the_app_prefix() {
    var rule = ArchitectureConditions.propertiesPrefixIsApp();
    assertFails(rule, Fixtures.WrongPrefix.class);
    assertPasses(rule, Fixtures.RightPrefix.class);
  }

  @Test
  void api02_checks_the_prefix_of_each_surface() {
    var rule = ArchitectureConditions.routesUseTheSurfacePrefix();
    assertFails(rule, CoreRoutes.WrongPrefixController.class);
    assertFails(rule, BffRoutes.BadBffController.class);
    assertPasses(rule, CoreRoutes.V1Controller.class, BffRoutes.GoodBffController.class);
  }

  @Test
  void api02_normalizesClassAndMethodSegmentsLikeSpring() {
    var rule = ArchitectureConditions.routesUseTheSurfacePrefix();
    assertPasses(
        rule, CoreRoutes.PathVariableController.class, CoreRoutes.NoLeadingSlashController.class);
    assertFails(rule, CoreRoutes.NoLeadingSlashWrongController.class);
  }

  @Test
  void join_addsTheMissingSlashAndCollapsesRepeats() {
    assertThat(ArchitectureConditions.join("/api/v1/things", "{id}"))
        .isEqualTo("/api/v1/things/{id}");
    assertThat(ArchitectureConditions.join("api/v1/things/", "/x")).isEqualTo("/api/v1/things/x");
    assertThat(ArchitectureConditions.join("/api/v1/things", "")).isEqualTo("/api/v1/things");
    assertThat(ArchitectureConditions.join("", "")).isEqualTo("/");
  }

  @Test
  void api03_requires_v1_next_to_a_newer_version() {
    var rule = ArchitectureConditions.newApiVersionsCoexistWithV1();
    assertFails(rule, CoreRoutes.OrphanV2Controller.class);
    assertPasses(rule, CoreRoutes.V1Controller.class, CoreRoutes.V2WithV1Controller.class);
  }

  @Test
  void arq03_rejects_a_domain_that_depends_on_application() {
    var rule = ArchitectureConditions.layersFollowTheTable("com.tarimatwasi.fixtures");
    assertFails(rule, BadDomain.class, Service.class, GoodDomain.class);
    assertPasses(rule, GoodDomain.class, Service.class);
  }

  @Test
  void tst05_requires_the_tested_class_next_to_the_test() {
    var rule = ArchitectureConditions.testsLiveWithTheirClass(Set.of());
    assertFails(rule, BadTest.class);
    assertPasses(rule, GoodTest.class, Good.class);
  }
}
