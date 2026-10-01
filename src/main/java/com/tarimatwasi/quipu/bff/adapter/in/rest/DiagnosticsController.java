package com.tarimatwasi.quipu.bff.adapter.in.rest;

import com.tarimatwasi.quipu.shared.adapter.out.email.ResendPingService;
import com.tarimatwasi.quipu.shared.adapter.out.storage.R2PingService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiagnosticsController {

  private final R2PingService r2PingService;
  private final ResendPingService resendPingService;

  public DiagnosticsController(R2PingService r2PingService, ResendPingService resendPingService) {
    this.r2PingService = r2PingService;
    this.resendPingService = resendPingService;
  }

  @GetMapping("/bff/diagnostics/ping-services")
  public Map<String, String> pingServices() {
    return Map.of(
        "r2", r2PingService.ping(),
        "resend", resendPingService.ping());
  }
}
