package de.neuland.pug4j;

import static org.junit.Assert.assertEquals;

import de.neuland.pug4j.expression.GraalJsExpressionHandler;
import de.neuland.pug4j.template.PugTemplate;
import de.neuland.pug4j.template.ReaderTemplateLoader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.jexl3.*;
import org.junit.Ignore;
import org.junit.Test;

public class RecordModelRenderTest {

  // Records used in tests
  record Address(String city, String zip) {}

  record Person(String name, int age) {

    public String bla() {
      return name + age;
    }
  }

  record PersonWithAddress(String name, int age, Address address) {}

  private PugConfiguration newGraalConfig(String pug) throws Exception {
    PugConfiguration config = new PugConfiguration();
    config.setExpressionHandler(new GraalJsExpressionHandler());
    ReaderTemplateLoader loader = new ReaderTemplateLoader(new StringReader(pug), "inline");
    config.setTemplateLoader(loader);
    return config;
  }

  private PugConfiguration newJexlConfig(String pug) throws Exception {
    PugConfiguration config = new PugConfiguration();
    ReaderTemplateLoader loader = new ReaderTemplateLoader(new StringReader(pug), "inline");
    config.setTemplateLoader(loader);
    return config;
  }

  @Test
  public void rendersRecordComponentsWithDotAccess() throws Exception {
    String pug = "h1= person.name\np= person.age";
    PugConfiguration config = newGraalConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("person", new Person("Alice", 42));

    String html = config.renderTemplate(template, model);
    // Depending on GraalJS member resolution, this may or may not work; keep as documentation of
    // desired behavior
    assertEquals("<h1>Alice</h1><p>42</p>", html);
  }

  @Test
  public void rendersNestedRecordComponentsWithDotAccess() throws Exception {
    String pug = "h1= person.address.city\np= person.address.zip";
    PugConfiguration config = newGraalConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("person", new PersonWithAddress("Alice", 42, new Address("Berlin", "10115")));

    String html = config.renderTemplate(template, model);
    assertEquals("<h1>Berlin</h1><p>10115</p>", html);
  }

  @Test
  @Ignore(
      "GraalJS does not support method call syntax for record components - use property access instead (person.name not person.name())")
  public void rendersRecordComponentsWithMethodCalls() throws Exception {
    String pug = "h1= person.name()\np= person.age()";
    PugConfiguration config = newGraalConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("person", new Person("Alice", 42));

    String html = config.renderTemplate(template, model);
    assertEquals("<h1>Alice</h1><p>42</p>", html);
  }

  @Test
  @Ignore(
      "GraalJS does not support method call syntax for records - use property access instead (person.address.city not person.address().city())")
  public void rendersNestedRecordComponentsWithMethodCalls() throws Exception {
    String pug = "h1= person.address().city()\np= person.address().zip()";
    PugConfiguration config = newGraalConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("person", new PersonWithAddress("Alice", 42, new Address("Berlin", "10115")));

    String html = config.renderTemplate(template, model);
    assertEquals("<h1>Berlin</h1><p>10115</p>", html);
  }

  @Test
  public void jexlRendersRecordComponentsWithDotAccess() throws Exception {
    String pug = "h1= person.name\np= person.age";
    PugConfiguration config = newJexlConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("person", new Person("Alice", 42));

    String html = config.renderTemplate(template, model);
    // Depending on GraalJS member resolution, this may or may not work; keep as documentation of
    // desired behavior
    assertEquals("<h1>Alice</h1><p>42</p>", html);
  }

  @Test
  public void jexlRendersNestedRecordComponentsWithDotAccess() throws Exception {
    String pug = "h1= person.address.city\np= person.address.zip";
    PugConfiguration config = newJexlConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("person", new PersonWithAddress("Alice", 42, new Address("Berlin", "10115")));

    String html = config.renderTemplate(template, model);
    assertEquals("<h1>Berlin</h1><p>10115</p>", html);
  }

  @Test
  public void jexlRendersRecordComponentsWithMethodCalls() throws Exception {
    String pug = "h1= person.name()\np= person.age()";
    PugConfiguration config = newJexlConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("person", new Person("Alice", 42));

    String html = config.renderTemplate(template, model);
    assertEquals("<h1>Alice</h1><p>42</p>", html);
  }

  @Test
  public void jexlRendersNestedRecordComponentsWithMethodCalls() throws Exception {
    String pug = "h1= person.address().city()\np= person.address().zip()";
    PugConfiguration config = newJexlConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("person", new PersonWithAddress("Alice", 42, new Address("Berlin", "10115")));

    String html = config.renderTemplate(template, model);
    assertEquals("<h1>Berlin</h1><p>10115</p>", html);
  }

  @Test
  public void jexlCallsMethodOnRecord() throws Exception {
    String pug = "h1= person.bla()";
    PugConfiguration config = newJexlConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();

    model.put("person", new Person("Alice", 42));

    String html = config.renderTemplate(template, model);
    assertEquals("<h1>Alice42</h1>", html);
  }

  @Test
  public void graalCallsMethodOnRecord() throws Exception {
    String pug = "h1= person.bla()";
    PugConfiguration config = newGraalConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();

    model.put("person", new Person("Alice", 42));

    String html = config.renderTemplate(template, model);
    assertEquals("<h1>Alice42</h1>", html);
  }

  @Test
  public void jexlHandlesRecordInConditional() throws Exception {
    String pug =
        """
                if person.age > 18
                  p Adult
                else
                  p Minor
                """;
    PugConfiguration config = newJexlConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("person", new Person("Alice", 42));

    String html = config.renderTemplate(template, model);
    assertEquals("<p>Adult</p>", html);
  }

  @Test
  public void graalHandlesRecordInConditional() throws Exception {
    String pug =
        """
                if person.age > 18
                  p Adult
                else
                  p Minor
                """;
    PugConfiguration config = newGraalConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("person", new Person("Alice", 42));

    String html = config.renderTemplate(template, model);
    assertEquals("<p>Adult</p>", html);
  }

  @Test
  public void jexlHandlesRecordWithNullComponent() throws Exception {
    String pug =
        """
                if person.address
                  p= person.address.city
                else
                  p No address
                """;
    PugConfiguration config = newJexlConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("person", new PersonWithAddress("Alice", 42, null));

    String html = config.renderTemplate(template, model);
    assertEquals("<p>No address</p>", html);
  }

  @Test
  public void graalHandlesRecordWithNullComponent() throws Exception {
    String pug =
        """
                if person.address
                  p= person.address.city
                else
                  p No address
                """;
    PugConfiguration config = newGraalConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("person", new PersonWithAddress("Alice", 42, null));

    String html = config.renderTemplate(template, model);
    assertEquals("<p>No address</p>", html);
  }

  @Test
  public void jexlHandlesRecordInInterpolation() throws Exception {
    String pug = "p Hello, #{person.name}! You are #{person.age} years old.";
    PugConfiguration config = newJexlConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("person", new Person("Alice", 42));

    String html = config.renderTemplate(template, model);
    assertEquals("<p>Hello, Alice! You are 42 years old.</p>", html);
  }

  @Test
  public void graalHandlesRecordInInterpolation() throws Exception {
    String pug = "p Hello, #{person.name}! You are #{person.age} years old.";
    PugConfiguration config = newGraalConfig(pug);
    PugTemplate template = config.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("person", new Person("Alice", 42));

    String html = config.renderTemplate(template, model);
    assertEquals("<p>Hello, Alice! You are 42 years old.</p>", html);
  }
}
