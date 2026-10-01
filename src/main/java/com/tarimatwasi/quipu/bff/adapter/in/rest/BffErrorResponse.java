package com.tarimatwasi.quipu.bff.adapter.in.rest;

import org.jspecify.annotations.Nullable;

public record BffErrorResponse(String code, String message, @Nullable String field) {
  public BffErrorResponse(String code, String message) {
    this(code, message, null);
  }
}
