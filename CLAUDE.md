# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## About Pug4j

Pug4j is a Java implementation of the Pug templating language (formerly Jade), providing full compatibility with Pug syntax without requiring a JavaScript environment. The project enables processing Pug templates in Java applications.

## Build Commands

### Core Build Commands
- `mvn compile` - Compile source code
- `mvn test` - Run unit tests
- `mvn verify` - Run full test suite with integration tests
- `mvn install` - Build and install to local repository
- `mvn clean` - Clean build artifacts

### Test Commands
- `mvn test` - Run all tests
- `mvn --batch-mode --update-snapshots verify` - Full verification with snapshot updates (used in CI)
- Single test: `mvn test -Dtest=ClassName`

### Release Commands
- `./mvn-release.sh` - Automated release script (requires GPG setup)

## Project Architecture

### Core Components
- **Lexer** (`lexer/`): Tokenizes Pug template files into a stream of tokens
- **Parser** (`parser/`): Converts tokens into an Abstract Syntax Tree (AST) of nodes  
- **Compiler** (`compiler/`): Transforms AST into HTML output with configurable formatting
- **Expression Handlers** (`expression/`): Evaluates expressions in templates
  - JexlExpressionHandler (default): Uses Apache Commons JEXL for JavaScript-like expressions
  - GraalJsExpressionHandler (experimental): Uses GraalVM for native JavaScript execution
- **Template Loaders** (`template/`): Handle template file loading from various sources
- **Filters** (`filter/`): Process embedded content (markdown, plain text, CDATA, etc.)

### Key Classes
- `Pug4J`: Main entry point for simple template rendering
- `PugConfiguration`: Central configuration object for advanced usage
- `PugTemplate`: Compiled template ready for rendering with different models
- `PugModel`: Model data container for template variables

### Template Loading Strategy
Templates are loaded through a base path system:
- FileTemplateLoader: Loads from filesystem with configurable base directory
- ClasspathTemplateLoader: Loads from classpath resources
- ReaderTemplateLoader: Loads from Reader instances

### Expression System
Two expression evaluation strategies:
- **JEXL** (default): Faster, JavaScript-like syntax with some limitations
- **GraalVM**: True JavaScript compatibility but significantly slower

### Caching
Built-in template caching using Caffeine cache:
- Templates cached by path and modification time
- Expression compilation results cached
- Configurable cache settings via PugConfiguration

## Development Notes

### Testing
- Extensive test suite with template files in `src/test/resources/`
- Snapshot testing for output verification
- Benchmark tests using JMH framework
- Cross-platform testing (Windows/Linux) in CI

### Dependencies
- Java 8+ required (builds with Java 8-17)
- Core dependencies: Apache Commons (JEXL, IO, Lang, Text), Caffeine cache
- Optional: GraalVM for JavaScript expression handling
- Build: Maven 3.0+

### Code Organization
- Package structure follows component boundaries
- Token classes in `lexer/token/`
- AST node classes in `parser/node/`
- Clear separation between lexing, parsing, and compilation phases

### Expression Handler Selection
Configure expression handling via PugConfiguration:
```java
// Default JEXL handler (recommended)
config.setExpressionHandler(new JexlExpressionHandler());

// GraalVM handler for JavaScript compatibility (slower)
config.setExpressionHandler(new GraalJsExpressionHandler());
```