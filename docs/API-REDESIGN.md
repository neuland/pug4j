# Pug4J 3.0.0 API Redesign

## Overview

This document describes the architectural changes in Pug4J 3.0.0, focusing on separating concerns and creating a more intuitive, flexible API.

## The Problem

The previous API (2.x) had several issues:

1. **`PugConfiguration` did too much:**
   - Template factory (getTemplate, caching)
   - Render options (prettyPrint, mode)
   - Render executor (renderTemplate method)

2. **Awkward rendering flow:**
   ```java
   PugTemplate template = config.getTemplate("index");
   String html = config.renderTemplate(template, model);  // Why?
   ```
   You get a template FROM config, then pass it BACK to config to render it.

3. **Unclear path resolution:**
   - What's the difference between `templateLoaderPath` and `basePath`?
   - How are absolute (`/layout`) vs relative (`../header`) references resolved?

4. **Confusing terminology:**
   - `mode` - Is it a template property or a fallback?
   - `sharedVariables` - Shared with what?

## The Solution

### 1. Separate Configuration from Rendering Context

Split `PugConfiguration` into three distinct classes:

#### A. `PugEngine` - Template Factory & Cache Manager

**Responsibilities:**
- Load and cache templates
- Manage template loader (file system, classpath, etc.)
- Manage expression handler (JEXL, GraalVM)
- Manage globally available filters

```java
public class PugEngine {
    private final TemplateLoader templateLoader;
    private final ExpressionHandler expressionHandler;
    private final boolean caching;
    private final Map<String, Filter> filters;

    // Template factory methods
    public PugTemplate getTemplate(String name) throws IOException
    public boolean templateExists(String name)
    public void clearCache()

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        public Builder templateLoader(TemplateLoader loader)
        public Builder expressionHandler(ExpressionHandler handler)
        public Builder caching(boolean enabled)
        public Builder filter(String name, Filter filter)
        public PugEngine build()
    }
}
```

**Example:**
```java
PugEngine engine = PugEngine.builder()
    .templateLoader(
        FileTemplateLoader.builder()
            .templateLoaderPath("/project/templates")
            .basePath("pages")
            .build()
    )
    .expressionHandler(new JexlExpressionHandler())
    .filter("markdown", new MarkdownFilter())
    .caching(true)
    .build();

PugTemplate template = engine.getTemplate("index");
```

#### B. `RenderContext` - Render-Time Options

**Responsibilities:**
- Pretty print setting
- Default/fallback mode for templates without doctype
- Global variables available to all renders

```java
public class RenderContext {
    private final boolean prettyPrint;
    private final Mode defaultMode;  // fallback when template has no doctype
    private final Map<String, Object> globalVariables;

    // Factory methods
    public static Builder builder()
    public static RenderContext defaults()  // prettyPrint=false, mode=HTML

    public static class Builder {
        public Builder prettyPrint(boolean pretty)
        public Builder defaultMode(Mode mode)
        public Builder globalVariable(String name, Object value)
        public Builder globalVariables(Map<String, Object> vars)
        public RenderContext build()
    }
}
```

**Example:**
```java
RenderContext context = RenderContext.builder()
    .prettyPrint(true)
    .defaultMode(Mode.HTML)
    .globalVariable("appName", "MyApp")
    .globalVariable("version", "1.0")
    .build();
```

#### C. `PugTemplate` - Self-Rendering Template

**New behavior:** Templates render themselves (no longer delegate to configuration).

```java
public class PugTemplate {
    private final Node rootNode;
    private final Mode intrinsicMode;  // from doctype, null if none

    // New rendering methods
    public String render(Map<String, Object> model, RenderContext context)
    public void render(Map<String, Object> model, RenderContext context, Writer writer)

    // Convenience
    public String render(Map<String, Object> model)  // uses RenderContext.defaults()

    // Read-only access
    public Node getRootNode()
    public Mode getIntrinsicMode()  // null if template has no doctype
}
```

**Example:**
```java
PugTemplate template = engine.getTemplate("index");
String html = template.render(model, context);  // Natural!
```

---

## Template Path Resolution

### Architecture

```
FileTemplateLoader
├── templateLoaderPath (absolute root directory)
│   └── basePath (subdirectory for absolute references)
│       ├── absolute references (/layout, /common/header)
│       └── relative references (../header, ./footer)
```

### Terminology

- **`templateLoaderPath`**: Absolute filesystem path to the root template directory
  - Example: `/project/src/main/resources/templates`
  - Set when creating `FileTemplateLoader`

- **`basePath`**: Subdirectory within `templateLoaderPath` used as prefix for absolute template references (those starting with `/`)
  - Example: `pages`
  - Optional, defaults to `""` (empty)

### Resolution Rules

#### Rule 1: Absolute References (start with `/`)

Template references starting with `/` are resolved as:
```
templateLoaderPath + basePath + reference
```

**Example:**
```java
FileTemplateLoader loader = FileTemplateLoader.builder()
    .templateLoaderPath("/project/templates")
    .basePath("pages")
    .build();
```

```pug
// In any template
extends /layout
include /common/header
```

**Resolution:**
- `/layout` → `/project/templates/pages/layout.pug`
- `/common/header` → `/project/templates/pages/common/header.pug`

#### Rule 2: Relative References (no leading `/`)

Template references NOT starting with `/` are resolved relative to the **current template's directory**.

**Example:**
```pug
// File: pages/users/list.pug
include ../common/header
extends ../../layout
```

**Resolution:**
- Parent template: `pages/users/list.pug`
- `../common/header` → `pages/common/header.pug`
- `../../layout` → `layout.pug`

### Why Two Paths?

This design provides flexibility:

1. **`templateLoaderPath`**: Security boundary - templates can only be loaded from this directory tree
2. **`basePath`**: Convenience - allows you to organize templates in subdirectories but reference common layouts/partials with absolute paths

**Use case:**
```
/project/templates/          ← templateLoaderPath
├── components/              ← basePath = "components"
│   ├── layout.pug
│   ├── header.pug
│   └── footer.pug
├── pages/
│   ├── home.pug            → extends /layout
│   └── about.pug           → extends /layout
└── admin/
    └── dashboard.pug       → extends /layout
```

All page templates can use `extends /layout` instead of `../../components/layout`.

---

## API Comparison

### Simple Static API (Unchanged)

```java
// 2.x and 3.0 (same)
String html = Pug4J.render("template.pug", model);
String html = Pug4J.render("template.pug", model, true);  // pretty print
```

### Advanced API

#### Version 2.x (Old)
```java
PugConfiguration config = new PugConfiguration();
config.setTemplateLoader(new FileTemplateLoader("/templates", "pug"));
config.setPrettyPrint(true);
config.setMode(Pug4J.Mode.HTML);
config.setSharedVariables(Map.of("app", "MyApp"));
config.setFilter("markdown", new MarkdownFilter());

PugTemplate template = config.getTemplate("index");
String html = config.renderTemplate(template, model);
```

#### Version 3.0 (New)
```java
// One-time setup
PugEngine engine = PugEngine.builder()
    .templateLoader(
        FileTemplateLoader.builder()
            .templateLoaderPath("/templates")
            .extension("pug")
            .build()
    )
    .filter("markdown", new MarkdownFilter())
    .caching(true)
    .build();

RenderContext context = RenderContext.builder()
    .prettyPrint(true)
    .defaultMode(Mode.HTML)
    .globalVariable("app", "MyApp")
    .build();

// Per-request
PugTemplate template = engine.getTemplate("index");
String html = template.render(model, context);
```

### Per-Request Context Variations

```java
PugTemplate template = engine.getTemplate("index");

// Pretty printed HTML
String html1 = template.render(model,
    RenderContext.builder().prettyPrint(true).build());

// Compact HTML
String html2 = template.render(model,
    RenderContext.defaults());

// XML mode
String xml = template.render(model,
    RenderContext.builder().defaultMode(Mode.XML).build());
```

---

## Migration Guide

### Option 1: Quick Migration (Keep Structure)

Replace `PugConfiguration` with `PugEngine` + `RenderContext`:

**Before:**
```java
PugConfiguration config = new PugConfiguration();
config.setTemplateLoader(loader);
config.setPrettyPrint(true);
config.setMode(Mode.HTML);

PugTemplate template = config.getTemplate("index");
String html = config.renderTemplate(template, model);
```

**After:**
```java
PugEngine engine = PugEngine.builder()
    .templateLoader(loader)
    .build();

RenderContext context = RenderContext.builder()
    .prettyPrint(true)
    .defaultMode(Mode.HTML)
    .build();

PugTemplate template = engine.getTemplate("index");
String html = template.render(model, context);
```

### Option 2: Optimize for Reuse

Separate one-time setup from per-request rendering:

**Before:**
```java
// Called on every request
PugConfiguration config = new PugConfiguration();  // Recreated each time!
config.setTemplateLoader(loader);
PugTemplate template = config.getTemplate("index");
String html = config.renderTemplate(template, model);
```

**After:**
```java
// One-time setup (e.g., in constructor or @PostConstruct)
PugEngine engine = PugEngine.builder()
    .templateLoader(loader)
    .caching(true)  // Enables template caching
    .build();

RenderContext defaultContext = RenderContext.defaults();

// Per-request (fast!)
PugTemplate template = engine.getTemplate("index");  // Cached!
String html = template.render(model, defaultContext);
```

### Option 3: Use Template Loader Builder

**Before:**
```java
FileTemplateLoader loader = new FileTemplateLoader(
    "/templates",
    StandardCharsets.UTF_8,
    "pug"
);
```

**After:**
```java
FileTemplateLoader loader = FileTemplateLoader.builder()
    .templateLoaderPath("/templates")
    .encoding(StandardCharsets.UTF_8)
    .extension("pug")
    .build();
```

---

## Key Terminology Changes

| 2.x | 3.0 | Rationale |
|-----|-----|-----------|
| `PugConfiguration` | `PugEngine` + `RenderContext` | Separate template factory from render options |
| `sharedVariables` | `globalVariables` | Clearer name |
| `mode` | `defaultMode` | Clarifies it's a fallback when template has no doctype |
| `template.process()` | `template.render()` | More intuitive |
| `config.renderTemplate(template, model)` | `template.render(model, context)` | Template renders itself |

---

## Benefits

1. **Clear Separation of Concerns**
   - `PugEngine`: Template management
   - `RenderContext`: Render-time options
   - `PugTemplate`: Self-contained rendering

2. **Natural API Flow**
   ```java
   template = engine.getTemplate(name)
   html = template.render(model, context)
   ```
   vs the old awkward:
   ```java
   template = config.getTemplate(name)
   html = config.renderTemplate(template, model)  // Why pass back?
   ```

3. **Flexible Rendering**
   Same template can be rendered with different contexts without reconfiguration

4. **Better Performance**
   Clear separation encourages proper engine/context reuse

5. **Builder Pattern**
   Fluent, readable configuration

6. **Clear Documentation**
   Path resolution rules are explicit and well-documented

---

## Breaking Changes

1. `PugConfiguration` class deprecated (to be removed in 4.0)
2. `PugTemplate.process()` deprecated (use `render()`)
3. Template loader constructors deprecated (use builders)
4. Some `Pug4J.render()` overloads deprecated (too many variations)
5. `sharedVariables` renamed to `globalVariables`
6. `mode` renamed to `defaultMode`

---

## Timeline

- **3.0.0**: New API introduced, old API deprecated
- **3.x**: Both APIs work (via adapter pattern)
- **4.0.0**: Deprecated API removed

---

## Questions & Answers

### Q: Why not keep `PugConfiguration`?

**A:** It mixed three distinct responsibilities:
- Template factory (engine)
- Render options (context)
- Render executor (now template's job)

The new design follows Single Responsibility Principle.

### Q: Do I need to create a new `RenderContext` for every request?

**A:** No! Create reusable contexts:
```java
RenderContext pretty = RenderContext.builder().prettyPrint(true).build();
RenderContext compact = RenderContext.defaults();

// Reuse
html1 = template.render(model1, pretty);
html2 = template.render(model2, compact);
```

### Q: What if I don't want to use builders?

**A:** We'll provide convenience factory methods:
```java
PugEngine engine = PugEngine.forPath("/templates");
RenderContext context = RenderContext.defaults();
```

### Q: Why is `defaultMode` in RenderContext instead of PugEngine?

**A:** Because mode is a render-time decision:
- Template's intrinsic mode (from doctype) takes precedence
- `defaultMode` is only a **fallback** when template has no doctype
- Different renders might want different fallbacks

Example:
```java
// API endpoint returning HTML
html = template.render(model, htmlContext);

// API endpoint returning XML
xml = template.render(model, xmlContext);
```

---

## Implementation Status

- [ ] `RenderContext` class
- [ ] `PugEngine` class
- [ ] `PugTemplate.render()` methods
- [ ] `FileTemplateLoader.Builder`
- [ ] `ClasspathTemplateLoader.Builder`
- [ ] Deprecate `PugConfiguration`
- [ ] Update documentation
- [ ] Migration guide
- [ ] Tests
