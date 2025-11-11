package de.neuland.pug4j.benchmark;

import de.neuland.pug4j.PugEngine;
import de.neuland.pug4j.RenderContext;
import de.neuland.pug4j.template.ClasspathTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.*;

@Fork(1)
@Warmup(iterations = 10)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
public class TemplateBenchmark {

  ClasspathTemplateLoader templateLoader = new ClasspathTemplateLoader();

  List<String> books = Arrays.asList("booka", "bookb", "bookc");

  @Param({"0", "1"})
  public int templateId;

  HashMap<String, Object> model = new HashMap<>();

  PugEngine pugEngine;

  @Setup(Level.Invocation)
  public void setUp() {
    pugEngine = PugEngine.builder().templateLoader(templateLoader).build();
    model.put("pageName", "Jade");
    model.put("books", books);
  }

  @Benchmark
  public void templates() throws Exception {
    Writer writer = new StringWriter();
    PugTemplate template = pugEngine.getTemplate("benchmark/simple" + templateId);
    pugEngine.render(template, model, RenderContext.defaults(), writer);
  }

  public static void main(String[] args) throws Exception {
    Main.main(args);
  }
}
