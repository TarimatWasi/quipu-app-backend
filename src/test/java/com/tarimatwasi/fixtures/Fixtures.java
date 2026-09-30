package com.tarimatwasi.fixtures;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.bind.annotation.RequestBody;

/** Classes that break (or respect) each custom rule; they are never part of the application. */
public final class Fixtures {

  private Fixtures() {}

  public static class TwoValues {
    public TwoValues(@Value("${a}") String a, @Value("${b}") String b) {}
  }

  public static class OneValue {
    public OneValue(@Value("${a}") String a) {}
  }

  public static class FieldValue {
    @Value("${a}")
    String a;
  }

  public static class SpelValue {
    public SpelValue(@Value("#{1 + 1}") int a) {}
  }

  public static class UnvalidatedBody {
    public void save(@RequestBody String body) {}
  }

  public static class ValidatedBody {
    public void save(@Valid @RequestBody String body) {}
  }

  @Entity
  public static class EagerRelation {
    @ManyToOne Object owner;
  }

  @Entity
  public static class LazyRelation {
    @ManyToOne(fetch = FetchType.LAZY)
    Object owner;
  }

  @Entity
  public static class EagerGetter {
    @ManyToOne
    public Object getOwner() {
      return null;
    }
  }

  @Entity
  public static class LazyGetter {
    @ManyToOne(fetch = FetchType.LAZY)
    public Object getOwner() {
      return null;
    }
  }

  public static class ThreadWithName {
    public final Thread thread = new Thread("w");
  }

  public static class ThreadWithTaskAndName {
    public final Thread thread = new Thread(() -> {}, "w");
  }

  public static class ThreadSubclassInstance {
    public final Thread thread = new Thread() {};
  }

  public static class ManagedThreadFree {
    public final Object value = new Object();
  }

  @ConfigurationProperties(prefix = "other.thing")
  public record WrongPrefix(String a) {}

  @ConfigurationProperties(prefix = "app.thing")
  public record RightPrefix(String a) {}
}
