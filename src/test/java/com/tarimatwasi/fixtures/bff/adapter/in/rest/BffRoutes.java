package com.tarimatwasi.fixtures.bff.adapter.in.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

public final class BffRoutes {

  private BffRoutes() {}

  @RestController
  public static class GoodBffController {
    @GetMapping("/bff/things")
    public String list() {
      return "";
    }
  }

  @RestController
  public static class BadBffController {
    @GetMapping("/api/v1/things")
    public String list() {
      return "";
    }
  }
}
