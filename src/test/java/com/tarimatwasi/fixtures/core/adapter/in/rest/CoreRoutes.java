package com.tarimatwasi.fixtures.core.adapter.in.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

public final class CoreRoutes {

  private CoreRoutes() {}

  @RestController
  @RequestMapping("/wrong")
  public static class WrongPrefixController {
    @GetMapping("/things")
    public String list() {
      return "";
    }
  }

  @RestController
  @RequestMapping("/api/v1/things")
  public static class V1Controller {
    @GetMapping
    public String list() {
      return "";
    }
  }

  @RestController
  @RequestMapping("/api/v2/orphans")
  public static class OrphanV2Controller {
    @GetMapping
    public String list() {
      return "";
    }
  }

  @RestController
  @RequestMapping("/api/v2/things")
  public static class V2WithV1Controller {
    @GetMapping
    public String list() {
      return "";
    }
  }
}
