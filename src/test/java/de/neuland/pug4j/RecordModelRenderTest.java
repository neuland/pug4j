package de.neuland.pug4j;

import de.neuland.pug4j.expression.GraalJsExpressionHandler;
import de.neuland.pug4j.template.PugTemplate;
import de.neuland.pug4j.template.ReaderTemplateLoader;
import org.apache.commons.jexl3.*;
import org.junit.Ignore;
import org.junit.Test;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RecordModelRenderTest {

    // Records used in tests
    record Address(String city, String zip) {}
    record Person(String name, int age) {}
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
        // Depending on GraalJS member resolution, this may or may not work; keep as documentation of desired behavior
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
    @Ignore
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
    @Ignore
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
    public void jexlrendersRecordComponentsWithDotAccess() throws Exception {
        String pug = "h1= person.name\np= person.age";
        PugConfiguration config = newJexlConfig(pug);
        PugTemplate template = config.getTemplate("inline");

        Map<String, Object> model = new HashMap<>();
        model.put("person", new Person("Alice", 42));

        String html = config.renderTemplate(template, model);
        // Depending on GraalJS member resolution, this may or may not work; keep as documentation of desired behavior
        assertEquals("<h1>Alice</h1><p>42</p>", html);
    }

    @Test
    public void jexlrendersNestedRecordComponentsWithDotAccess() throws Exception {
        String pug = "h1= person.address.city\np= person.address.zip";
        PugConfiguration config = newJexlConfig(pug);
        PugTemplate template = config.getTemplate("inline");

        Map<String, Object> model = new HashMap<>();
        model.put("person", new PersonWithAddress("Alice", 42, new Address("Berlin", "10115")));

        String html = config.renderTemplate(template, model);
        assertEquals("<h1>Berlin</h1><p>10115</p>", html);
    }

    @Test
    @Ignore
    public void jexlrendersRecordComponentsWithMethodCalls() throws Exception {
        String pug = "h1= person.name()\np= person.age()";
        PugConfiguration config = newJexlConfig(pug);
        PugTemplate template = config.getTemplate("inline");

        Map<String, Object> model = new HashMap<>();
        model.put("person", new Person("Alice", 42));

        String html = config.renderTemplate(template, model);
        assertEquals("<h1>Alice</h1><p>42</p>", html);
    }

    @Test
    @Ignore
    public void jexlrendersNestedRecordComponentsWithMethodCalls() throws Exception {
        String pug = "h1= person.address().city()\np= person.address().zip()";
        PugConfiguration config = newJexlConfig(pug);
        PugTemplate template = config.getTemplate("inline");

        Map<String, Object> model = new HashMap<>();
        model.put("person", new PersonWithAddress("Alice", 42, new Address("Berlin", "10115")));

        String html = config.renderTemplate(template, model);
        assertEquals("<h1>Berlin</h1><p>10115</p>", html);
    }
    @Test
    @Ignore
    public void jexlTest() throws Exception {
        JexlEngine jexl = new JexlBuilder().cache(512).strict(true).silent(false).create();
//        String pug = "person.address().city()";
        String pug = "person.name()";
        final JexlExpression expression = jexl.createExpression(pug);
        JexlContext context = new MapContext();
        context.set("person", new PersonWithAddress("Alice", 42, new Address("Berlin", "10115")));
        context.set("a", "bla");
        final Object evaluate = expression.evaluate(context);
        assertEquals("<h1>Berlin</h1><p>10115</p>", evaluate.toString());
    }

}
