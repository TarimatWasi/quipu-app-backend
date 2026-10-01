package com.tarimatwasi.quipu.bff.adapter.in.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarimatwasi.quipu.shared.adapter.out.email.ResendPingService;
import com.tarimatwasi.quipu.shared.adapter.out.storage.R2PingService;
import com.tarimatwasi.quipu.support.PostgresContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** The diagnostics report each service independently (security is covered separately). */
@SpringBootTest
@ImportTestcontainers(PostgresContainers.class)
class DiagnosticsControllerTest {

  @Autowired WebApplicationContext webApplicationContext;
  @MockitoBean R2PingService r2PingService;
  @MockitoBean ResendPingService resendPingService;
  MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    // No security filters on purpose: this test is about the controller, not about who may call it.
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  void reportsEachServiceIndependentlyWhenOneFails() throws Exception {
    when(r2PingService.ping()).thenReturn("OK");
    when(resendPingService.ping()).thenReturn("FAIL: 401 Unauthorized");

    mockMvc
        .perform(get("/bff/diagnostics/ping-services"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.r2").value("OK"))
        .andExpect(jsonPath("$.resend").value("FAIL: 401 Unauthorized"));
  }
}
