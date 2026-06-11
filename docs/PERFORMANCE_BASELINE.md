# Performance Baseline: Expression Handler Rendering

Baseline measurements for the planned GraalJS model-binding rework (model bound
directly as a `with`-scope ProxyObject instead of copying all model entries
into the JS global bindings and writing them back on every expression
evaluation — the mechanism JEXL effectively gets for free via `JexlContext`).

Use these numbers to quantify the improvement once the rework lands, and for
the release notes of the version shipping it.

## Environment

| | |
|---|---|
| Date | 2026-06-11 |
| Machine | Apple M1 Max, 64 GB |
| JDK | OpenJDK 21.0.11 |
| Baseline commit | `9e954c0` (branch `improve-performance-of-graaljs`) |

## Workload 1: Kitchen-sink template (realistic, ~50 expressions/render)

Template: `src/test/resources/kitchensink/kitchen-sink.pug` — inheritance,
mixins, iteration, conditionals, filters, all attribute styles; model-driven
expressions (nested maps/lists, 7 top-level keys).

Reproduce:

```
mvn test -Dtest=KitchenSinkIntegrationTest -Dpug4j.kitchensink.perf=true
```

| Handler | 1000 renders | per render | vs JEXL |
|---|---:|---:|---:|
| JexlExpressionHandler | 149 ms | ~0.15 ms | 1× |
| GraalJsExpressionHandler | 27,610 ms | ~27.6 ms | ~185× slower |

After rework (fill in):

| Handler | 1000 renders | per render | vs baseline |
|---|---:|---:|---:|
| GraalJsExpressionHandler | | | |

## Workload 2: Single-expression templates (GraalJS, micro)

10,000 renders of one-line templates, 2,000 warmup, via a throwaway probe
(render loop over `ReaderTemplateLoader` + `PugEngine`, same shape as the
kitchen-sink perf smoke). Model: one record (3 components, nested record),
list of 20 records.

| Template | Baseline (9e954c0) |
|---|---:|
| `p= person.name` (property) | 986 ms |
| `p= person.name()` (rewritten accessor call, cached) | 905 ms |
| `each` over 20 records, 1,000 renders | 2,585 ms |

≈100 µs per single-expression render; profiling attributes most of it to the
per-expression model→bindings copy (`GraalJsExpressionHandler.evaluateExpression`,
model entry loop) and the bindings→model write-back
(`writeBackBindingsToModel`). This is the overhead the `with`-proxy rework
removes: cost becomes proportional to the variables an expression actually
references instead of the model size.

## Feasibility note

`with` + `ProxyObject` as scope was verified to work in GraalJS (see
`WithProxyScopeProbe`, 2026-06-11): lazy reads through the proxy, writes to
existing keys hit the model directly, unknown identifiers still raise
`ReferenceError`, builtins are not shadowed, and the `with`-wrapped parsed
Source is cacheable and re-executes against the live model. pug.js compiles
templates with `with (locals || {})`, so this also moves pug4j closer to
reference behavior. Known limitation: `with` is illegal in strict-mode code.
