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

After rework (with-scope ProxyObject model binding, 2026-06-11):

| Handler | 1000 renders | per render | vs baseline |
|---|---:|---:|---:|
| GraalJsExpressionHandler | 577 ms | ~0.58 ms | **~48× faster** |
| JexlExpressionHandler (re-run, unchanged) | 151 ms | ~0.15 ms | — |

GraalJS goes from ~185× slower than JEXL to ~3.8× slower.

## Workload 2: Single-expression templates (GraalJS, micro)

10,000 renders of one-line templates, 2,000 warmup, via a throwaway probe
(render loop over `ReaderTemplateLoader` + `PugEngine`, same shape as the
kitchen-sink perf smoke). Model: one record (3 components, nested record),
list of 20 records.

| Template | Baseline (9e954c0) | After rework | Speedup |
|---|---:|---:|---:|
| `p= person.name` (property) | 986 ms | 255 ms | 3.9× |
| `p= person.name()` (rewritten accessor call, cached) | 905 ms | 159 ms | 5.7× |
| `each` over 20 records, 1,000 renders | 2,585 ms | 208 ms | 12.4× |

≈100 µs per single-expression render; profiling attributes most of it to the
per-expression model→bindings copy (`GraalJsExpressionHandler.evaluateExpression`,
model entry loop) and the bindings→model write-back
(`writeBackBindingsToModel`). This is the overhead the `with`-proxy rework
removes: cost becomes proportional to the variables an expression actually
references instead of the model size.

## Graal JIT compiler experiment (jargraal, 2026-06-11)

Tested whether enabling Truffle runtime compilation (Graal compiler via
`--upgrade-module-path` on a stock Temurin JDK) speeds up rendering further
after the with-scope rework. Setup that worked:

- Temurin 25 + `org.graalvm.compiler:compiler:25.0.3` (+ `truffle-compiler`,
  `word`, `collections`, `nativeimage`) on `--upgrade-module-path`
- `-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI`
- `-Ddebug.jdk.graal.jvmciConfigCheck=warn` — Temurin's JVMCI lacks a VM
  config value (`NMethodPatchingType::conc_data_patch`) the Graal compiler
  expects, hard failure without the override
- Compiler version must match the JDK's JVMCI version (25.0.0 jars failed
  against Temurin 25.0.3); Temurin 21 fails earlier (qualified JVMCI exports
  target the old `jdk.internal.vm.compiler` module name)

Results (warm steady state, direct `java` runs):

| Workload | Interpreter | jargraal | Speedup |
|---|---:|---:|---:|
| Kitchen-sink, 1000 renders | ~143 ms | ~134 ms | ~6% |
| Compute-heavy JS expression (200k-iteration loop), 50 renders | ~240 ms | ~14 ms | ~17× |

Conclusion: after the with-scope rework, typical template rendering is bound
by host interop (proxy calls, value conversion, context enter/leave), not by
JS execution — the JIT barely helps. It only pays off for compute-heavy JS
inside expressions, which is rare in templates. Given the deployment friction
(flag soup, exact version coupling, config-check override on non-GraalVM
JDKs), this is a documentation-level recommendation for affected users, not
something pug4j should wire up.

## Feasibility note

`with` + `ProxyObject` as scope was verified to work in GraalJS (see
`WithProxyScopeProbe`, 2026-06-11): lazy reads through the proxy, writes to
existing keys hit the model directly, unknown identifiers still raise
`ReferenceError`, builtins are not shadowed, and the `with`-wrapped parsed
Source is cacheable and re-executes against the live model. pug.js compiles
templates with `with (locals || {})`, so this also moves pug4j closer to
reference behavior. Known limitation: `with` is illegal in strict-mode code.
