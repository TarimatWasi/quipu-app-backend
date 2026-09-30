package com.tarimatwasi.quipu.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarimatwasi.quipu.support.PostgresContainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * BE-SPR-WEB-04 and WEB-05: errors are {@code application/problem+json} and leak no internals. The
 * per-surface formats of QP-SPRMONO-BFF-01 and API-01 are added with the first controllers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ImportTestcontainers(PostgresContainers.class)
class ErrorContractTest {

  @Autowired MockMvc mvc;

  @Test
  @WithMockUser
  void unknown_route_returns_problem_detail_without_internals() throws Exception {
    var result =
        mvc.perform(get("/api/v1/does-not-exist")).andExpect(status().isNotFound()).andReturn();

    assertThat(result.getResponse().getContentType())
        .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(result.getResponse().getContentAsString())
        .doesNotContain("trace", "exception", "org.springframework");
  }
}
