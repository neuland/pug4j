package de.neuland.pug4j.expression;

import de.neuland.pug4j.PugEngine;
import de.neuland.pug4j.RenderContext;
import de.neuland.pug4j.template.PugTemplate;
import de.neuland.pug4j.template.ReaderTemplateLoader;
import org.junit.Test;

import java.io.StringReader;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RecordsJexlTest {

    // Define tiny records for tests (Java 17+)
    record Address(String city, String street) {}
    record Person(String name, int age, Address address) {}

    private String render(String pug, Map<String, Object> model) throws Exception {
        ReaderTemplateLoader loader = new ReaderTemplateLoader(new StringReader(pug), "inline");
        PugEngine engine = PugEngine.builder()
                .templateLoader(loader)
                .expressionHandler(new JexlExpressionHandler())
                .build();
        PugTemplate template = engine.getTemplate("inline");
        RenderContext context = RenderContext.defaults();
        return engine.render(template, model, context);
    }

    @Test
    public void record_component_dot_access_works() throws Exception {
        Person person = new Person("Alice", 41, new Address("Hamburg", "Reeperbahn"));
        Map<String, Object> model = new HashMap<>();
        model.put("person", person);

        String html = render("p= person.name", model);
        assertThat(html, is("<p>Alice</p>"));

        html = render("p= person.age + 1", model);
        assertThat(html, is("<p>42</p>"));
    }

    @Test
    public void record_component_parens_and_dot_both_work() throws Exception {
        Person person = new Person("Bob", 19, new Address("Berlin", "Unter den Linden"));
        Map<String, Object> model = new HashMap<>();
        model.put("person", person);

        // In JEXL, record accessors are real zero-arg methods, so both styles work
        String html = render("p= person.name()", model);
        assertThat(html, is("<p>Bob</p>"));

        html = render("p= person.age() + 1", model);
        assertThat(html, is("<p>20</p>"));
    }

    @Test
    public void nested_record_access_dot_and_parens() throws Exception {
        Person person = new Person("Clara", 30, new Address("Cologne", "Domplatz"));
        Map<String, Object> model = new HashMap<>();
        model.put("person", person);

        String html = render("p= person.address.city", model);
        assertThat(html, is("<p>Cologne</p>"));

        html = render("p= person.address.city()", model);
        assertThat(html, is("<p>Cologne</p>"));
    }

    @Test
    public void list_of_records_in_each_loop() throws Exception {
        List<Person> people = Arrays.asList(
                new Person("Ann", 21, new Address("Bonn", "Mitte")),
                new Person("Eve", 25, new Address("Munich", "Marienplatz"))
        );
        Map<String, Object> model = new HashMap<>();
        model.put("people", people);

        String pug = String.join("\n",
                "ul",
                "  each p in people",
                "    li= p.name + ' (' + p.age + ')'"
        );
        String html = render(pug, model);
        assertThat(html, is("<ul><li>Ann (21)</li><li>Eve (25)</li></ul>"));
    }

    public static class Pojo {
        private final String fullName;
        public Pojo(String fullName) { this.fullName = fullName; }
        public String fullName() { return fullName; }
    }

    @Test
    public void real_zero_arg_method_calls_on_pojo_work_normally() throws Exception {
        Pojo pojo = new Pojo("Dora Explorer");
        Map<String, Object> model = new HashMap<>();
        model.put("pojo", pojo);

        String html = render("p= pojo.fullName()", model);
        assertThat(html, is("<p>Dora Explorer</p>"));
    }

    @Test
    public void missing_component_is_null_and_renders_empty_string() throws Exception {
        Person person = new Person("Felix", 7, new Address("Essen", "Zentrum"));
        Map<String, Object> model = new HashMap<>();
        model.put("person", person);

        // Unknown property should evaluate to null and render as empty string
        String html = render("p= person.unknown", model);
        assertThat(html, is("<p></p>"));
    }
}
