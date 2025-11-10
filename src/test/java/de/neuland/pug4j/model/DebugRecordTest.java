package de.neuland.pug4j.model;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.template.PugTemplate;
import de.neuland.pug4j.template.ReaderTemplateLoader;
import org.junit.Test;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class DebugRecordTest {

    record Person(String name, int age) {}

    @Test
    public void debugJexl() throws Exception {
        String pug = "h1= person.name\np= person.age";
        PugConfiguration config = new PugConfiguration();
        ReaderTemplateLoader loader = new ReaderTemplateLoader(new StringReader(pug), "inline");
        config.setTemplateLoader(loader);
        PugTemplate template = config.getTemplate("inline");

        Map<String, Object> model = new HashMap<>();
        model.put("person", new Person("Alice", 42));

        // Create PugModel to see what it contains
        PugModel pugModel = new PugModel(model);
        System.out.println("Model contains: " + pugModel.keySet());
        System.out.println("Person object type: " + pugModel.get("person").getClass().getName());
        System.out.println("Person is RecordWrapper: " + (pugModel.get("person") instanceof RecordWrapper));
        if (pugModel.get("person") instanceof RecordWrapper) {
            RecordWrapper wrapper = (RecordWrapper) pugModel.get("person");
            System.out.println("Wrapper keys: " + wrapper.keySet());
            System.out.println("Wrapper name: " + wrapper.get("name"));
        }

        String html = config.renderTemplate(template, model);
        System.out.println("HTML: " + html);
        System.out.println("Expected: <h1>Alice</h1><p>42</p>");
    }
}
