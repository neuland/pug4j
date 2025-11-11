[![Test Status](https://github.com/neuland/pug4j/actions/workflows/test.yaml/badge.svg)](https://github.com/neuland/pug4j/actions)

# pug4j - a pug implementation written in Java
pug4j's intention is to be able to process pug templates in Java without the need of a JavaScript environment, while being **fully compatible** with the original pug syntax.

pug4j was formerly known as jade4j. Because of the naming change of the javascript version and the alignment to the featureset of pug.js (https://pugjs.org/) we decided to switch the name.
 
## Contents

- [Example](#example)
- [Syntax](#syntax)
- [Usage](#usage)
- [Simple static API](#simple-api)
- [Full API](#api)
    - [Caching](#api-caching)
    - [Output Formatting](#api-output)
    - [Filters](#api-filters)
    - [Helpers](#api-helpers)
    - [Model Defaults](#api-model-defaults)
    - [Java Records Support](#api-records)
    - [Template Loader](#api-template-loader)
- [Expressions](#expressions)
- [Reserved Words](#reserved-words)
- [Framework Integrations](#framework-integrations)
- [Breaking Changes in 3.0.0](#breaking-changes-3)
- [Breaking Changes in 2.0.0](#breaking-changes-2)
- [Breaking Changes in 1.0.0](#breaking-changes-1)
- [Authors](#authors)
- [License](#license)


## Example

index.pug

```
doctype html
html
  head
    title= pageName
  body
    ol#books
      for book in books
        if book.available
          li #{book.name} for #{book.price} €
```

Java model

```java
List<Book> books = new ArrayList<Book>();
books.add(new Book("The Hitchhiker's Guide to the Galaxy", 5.70, true));
books.add(new Book("Life, the Universe and Everything", 5.60, false));
books.add(new Book("The Restaurant at the End of the Universe", 5.40, true));

Map<String, Object> model = new HashMap<String, Object>();
model.put("books", books);
model.put("pageName", "My Bookshelf");
```

Running the above code through `String html = Pug4J.render("./index.pug", model)` will result in the following output:

```html
<!DOCTYPE html>
<html>
  <head>
    <title>My Bookshelf</title>
  </head>
  <body>
    <ol id="books">
      <li>The Hitchhiker's Guide to the Galaxy for 5,70 €</li>
      <li>The Restaurant at the End of the Universe for 5,40 €</li>
    </ol>
  </body>
</html>
```

## Syntax

See also the original [https://github.com/pugjs/pug#syntax](https://github.com/pugjs/pug#syntax).

## Usage

### via Maven

Just add following dependency definitions to your `pom.xml`.

```xml
<dependency>
  <groupId>de.neuland-bfi</groupId>
  <artifactId>pug4j</artifactId>
  <version>3.0.0</version>
</dependency>
```

### Build it yourself

Clone this repository ...

```bash
git clone https://github.com/neuland/pug4j.git
```

... build it using `maven` ...

```bash
cd pug4j
mvn install
```

... and use the `pug4j-2.x.x.jar` located in your target directory.

<a name="simple-api"></a>
## Simple static API

The simple static API provides the easiest way to render templates in one step:

```java
String html = Pug4J.render("./index.pug", model);
```

With pretty printing:

```java
String html = Pug4J.render("./index.pug", model, true);
```

Streaming output using a `java.io.Writer`:

```java
Pug4J.render("./index.pug", model, writer);
```

**Note:** For production use with template reuse, use the Full API below which provides caching and more control.

<a name="api"></a>
## Full API

For production use, create a `PugEngine` instance with the builder pattern. This separates template loading/caching (engine) from render-time settings (context).

**Quick setup:**

```java
PugEngine engine = PugEngine.forPath("/templates/");
PugTemplate template = engine.getTemplate("index.pug");
String html = engine.render(template, model);
```

**Advanced configuration:**

```java
// Configure template loading
FileTemplateLoader loader = new FileTemplateLoader("/root/dir/");
loader.setBase("base/path");

// Build engine with settings
PugEngine engine = PugEngine.builder()
    .templateLoader(loader)
    .caching(true)
    .build();

// Get template (cached automatically)
PugTemplate template = engine.getTemplate("index.pug");

// Prepare model
Map<String, Object> model = new HashMap<>();
model.put("company", "neuland");

// Render with default settings
String html = engine.render(template, model);

// Or with custom render context
RenderContext context = RenderContext.builder()
    .prettyPrint(true)
    .defaultMode(Pug4J.Mode.HTML)
    .build();

String prettyHtml = engine.render(template, model, context);
```

**Key concepts:**
- **PugEngine**: Immutable template factory - configure once, use many times
- **RenderContext**: Immutable render settings - can be reused or created per-render
- **PugTemplate**: Parsed template - obtained from engine, rendered via engine

<a name="api-caching"></a>
### Caching

`PugEngine` handles template caching automatically. If you request the same unmodified template twice, you'll get the same instance and avoid unnecessary parsing.

```java
PugTemplate t1 = engine.getTemplate("index.pug");
PugTemplate t2 = engine.getTemplate("index.pug");
t1.equals(t2) // true
```

Clear the template and expression cache:

```java
engine.clearCache();
```

Configure caching at build time:

```java
PugEngine engine = PugEngine.builder()
    .caching(false)  // Disable for development
    .maxCacheSize(500)  // Limit cache size
    .expressionCacheSize(1000)  // Configure expression cache
    .build();
```

<a name="api-output"></a>
### Output Formatting

By default, Pug4J produces compressed HTML without unneeded whitespace. You can enable pretty printing using `RenderContext`:

```java
RenderContext context = RenderContext.builder()
    .prettyPrint(true)
    .build();

String html = engine.render(template, model, context);
```

Pug detects if it has to generate (X)HTML or XML code by your specified [doctype](https://github.com/pugjs/pug#syntax).

If you are rendering partial templates that don't include a doctype, pug4j generates HTML code. You can set the `defaultMode` manually:

```java
RenderContext context = RenderContext.builder()
    .defaultMode(Pug4J.Mode.HTML)   // <input checked>
    .defaultMode(Pug4J.Mode.XHTML)  // <input checked="true" />
    .defaultMode(Pug4J.Mode.XML)    // <input checked="true"></input>
    .build();
```

<a name="api-filters"></a>
### Filters

Filters allow embedding content like `markdown` into your pug template:

    :markdown
      # headline
      hello **world**

will generate

    <h1>headline</h1>
    <p>hello <strong>world</strong></p>

pug4j comes with built-in `cdata`, `css`, and `js` filters. You can add custom filters when building your engine:

```java
PugEngine engine = PugEngine.builder()
    .filter("markdown", new MarkdownFilter())
    .build();
```

To implement your own filter, you have to implement the `Filter` Interface. If your filter doesn't use any data from the model you can inherit from the abstract `CachingFilter` and also get caching for free. See the [neuland/jade4j-coffeescript-filter](https://github.com/neuland/jade4j-coffeescript-filter) project as an example.

<a name="api-helpers"></a>
### Helpers

If you need to call custom java functions the easiest way is to create helper classes and put an instance into the model.

```java
public class MathHelper {
    public long round(double number) {
        return Math.round(number);
    }
}
```

```java
model.put("math", new MathHelper());
```

Note: Helpers don't have their own namespace, so you have to be careful not to overwrite them with other variables.

```
p= math.round(1.44)
```

<a name="api-model-defaults"></a>
### Model Defaults (Global Variables)

If you are using multiple templates, you might need default objects available in all renders. Use `globalVariables` in `RenderContext`:

```java
Map<String, Object> globals = new HashMap<>();
globals.put("city", "Bremen");
globals.put("country", "Germany");
globals.put("url", new MyUrlHelper());

RenderContext context = RenderContext.builder()
    .globalVariables(globals)
    .build();

// These variables are now available in every render using this context
String html = engine.render(template, model, context);
```

<a name="api-records"></a>
### Java Records Support

Since version 3.0.0, pug4j supports Java records as model objects. Records are automatically wrapped to make their components accessible in templates using property syntax, just like with Maps or POJOs.

**Example:**

```java
// Define records
record Author(String name, String email) {}
record Book(String title, double price, boolean available, Author author) {}

// Create model with records
Author author = new Author("Douglas Adams", "douglas@example.com");
Book book = new Book("The Hitchhiker's Guide to the Galaxy", 5.70, true, author);

Map<String, Object> model = new HashMap<>();
model.put("book", book);
```

**Template usage:**

```pug
div
  h1= book.title
  p Price: #{book.price} €
  if book.available
    p In stock!
  p Author: #{book.author.name} (#{book.author.email})
```

**Key features:**
- **Automatic wrapping**: Records are automatically wrapped when added to the model
- **Nested records**: Nested records are fully supported (e.g., `book.author.name`)
- **Property access**: Use dot notation (`book.title`) instead of method calls (`book.title()`)
- **Custom methods**: Records with custom methods can call them using function syntax (`record.customMethod()`)
- **Both expression handlers**: Works with both JEXL (default) and GraalJS expression handlers
- **Immutable**: Records remain immutable in templates

**Requirements:**
- Java 17 or higher (required for record support)

<a name="api-template-loader"></a>
### Template Loader

By default, pug4j searches for template files in your work directory. By specifying your own `FileTemplateLoader`, you can alter that behavior. You can also implement the `TemplateLoader` interface to create your own.

```java
TemplateLoader loader = new FileTemplateLoader("/templates/", "UTF-8");
loader.setBase("my-maintemplates/");

PugEngine engine = PugEngine.builder()
    .templateLoader(loader)
    .build();
```

* `/index` points to `/templates/my-maintemplates/index.pug`
* `index` points to `/templates/index.pug`

**Available loaders:**
- `FileTemplateLoader` - loads from filesystem
- `ClasspathTemplateLoader` - loads from classpath
- `ReaderTemplateLoader` - loads from Reader instances

You can also implement the `TemplateLoader` interface to create your own custom loader.

<a name="expressions"></a>
## Expressions

The original pug implementation uses JavaScript for expression handling in `if`, `unless`, `for`, `case` commands, like this

    - var book = {"price": 4.99, "title": "The Book"}
    if book.price < 5.50 && !book.soldOut
      p.sale special offer: #{book.title}

    each author in ["artur", "stefan", "michael","christoph"]
      h2= author

### Jexl Expressionhandler (default)
Pug4j uses [JEXL](https://commons.apache.org/proper/commons-jexl/) for parsing and executing these expressions.
JEXL syntax and behavior is very similar to ECMAScript/JavaScript and so closer to the original pug.js implementation. JEXL runs also much faster than GraalVM.
If your template don't relies too much on Javascript-Logic and gets almost everything from the model, this is a good choice.

We are using a slightly modified JEXL version which to have better control of the exception handling. JEXL now runs in a semi-strict mode, where non existing values and properties silently evaluate to `null`/`false` where as invalid method calls lead to a `PugCompilerException`.
<a name="reserved-words"></a>
#### Reserved Words

JEXL comes with the three builtin functions `new`, `size` and `empty`. For properties with this name the `.` notation does not work, but you can access them with `[]`.

```
- var book = {size: 540}
book.size // does not work
book["size"] // works
```

You can read more about this in the [JEXL documentation](https://commons.apache.org/proper/commons-jexl/reference/syntax.html#Language_Elements).

<a name="graalvm"></a>
### GraalVM Expressionhandler (since 2.0.0 / experimental!)
If you want to use pure JavaScript expression handling, you can use the GraalJS Expression Handler. It supports native JavaScript expressions but is significantly slower than the JEXL Expression Handler. Configure it when building your engine:

```java
PugEngine engine = PugEngine.builder()
    .expressionHandler(new GraalJsExpressionHandler())
    .build();
```


<a name="framework-integrations"></a>
## Framework Integrations
- [neuland/spring-pug4j](https://github.com/neuland/spring-pug4j) pug4j spring integration.
- [vertx-web](https://vertx.io/docs/vertx-web/java/#_jade_template_engine) jade4j for [Vert.X](http://vertx.io/)

<a name="breaking-changes-3"></a>
## Breaking Changes in 3.0.0

### API Redesign
- **`PugConfiguration` deprecated**: Replaced by immutable `PugEngine` and `RenderContext`
  - **Migration required**: See examples in Full API section above
  - Old API still works but will be removed in 4.0.0
- **Templates no longer self-render**: Use `engine.render(template, model, context)` instead of `template.render(model, context)`
- **Simple API changes**:
  - `Pug4J.getTemplate()` methods deprecated (not part of simple API)
  - Template-based `Pug4J.render(template, ...)` methods deprecated
  - Use `Pug4J.render(filename, ...)` for simple cases or `PugEngine` for advanced use

### Other Breaking Changes
- **Java 17+ required**: Minimum Java version raised from Java 8 to Java 17
- **Dependency updates**:
  - GraalVM updated to 25.0.0 (simplified dependencies using `polyglot` and `js-community`)
  - Caffeine cache updated to 3.2.2
  - Flexmark updated to 0.64.8

### New Features
- **Java records support**: Records are now automatically wrapped when added to model
- **Immutable builder pattern**: Thread-safe configuration with builder pattern
- **Better separation of concerns**: Template loading, caching, and rendering are clearly separated

<a name="breaking-changes-2"></a>
## Breaking Changes in 2.0.0
- Classes are renamed to pug4j.
- Default file extension is now .pug
- Compiler Level has been raised to Java 8+
- Syntax has been adapted to the most current pug version. (2.0.4)
- Filter interface changed
- Interpolations not supported in attributes and filters anymore. It now behaves the same way as in pug.js.

<a name="breaking-changes-1"></a>
### Breaking Changes in 1.3.1
- Fixed a mayor scoping bug in loops. Use this version and not 1.3.0

### Breaking Changes in 1.3.0
- setBasePath has been removed from JadeConfiguration. Set folderPath on FileTemplateLoader instead.
- Scoping of variables in loops changed, so its more in line with jade. This could break your template.

### Breaking Changes in 1.2.0
- Breaking change in filter interface: if you use filters outside of the project, they need to be adapted to new interface

### Breaking Changes in 1.0.0
In Version 1.0.0 we added a lot of features of JadeJs 1.11. There are also some Breaking Changes:
- Instead of 'id = 5' you must use '- var id = 5'
- Instead of 'h1(attributes, class = "test")' you must use 'h1(class= "test")&attributes(attributes)'
- Instead of '!!! 5' you must use 'doctype html'
- Jade Syntax for Conditional Comments is not supported anymore
- Thanks to rzara for contributing to issue-108


<a name="authors"></a>
## Authors

- Artur Tomas / [atomiccoder](https://github.com/atomiccoder)
- Stefan Kuper / [planetk](https://github.com/planetk)
- Michael Geers / [naltatis](https://github.com/naltatis)
- Christoph Blömer / [chbloemer](https://github.com/chbloemer)

Special thanks to [TJ Holowaychuk](https://github.com/visionmedia) the creator of jade!

<a name="license"></a>
## License

The MIT License

Copyright (C) 2011-2025 [neuland Büro für Informatik](http://www.neuland-bfi.de/), Bremen, Germany

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
