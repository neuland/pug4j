# History
## 2.0.6 / 2022-10-24
* updated libraries (commons-text to 1.10.0 - CVE-2022-42889)

## 2.0.5 / 2022-02-02
* refactoring filepathhandling to support windows
* fixed tests to run on windows platform

## 2.0.4 / 2021-12-13
* update gson dependency to the latest available v2.8.7 -> v2.8.9 (thanks dbelyaev)
* other dependency updates
  * flexmark 0.60.2 -> 0.62.2
  * caffeine 2.9.2 -> 2.9.3
  * graalvm 21.2.0 -> 21.3.0
  * slf4j-simple 1.7.31 -> 1.7.32
  * jmh-core 1.23 -> 1.33
  * jmh-generator-annprocess 1.23 -> 1.33

## 2.0.3 / 2021-09-07
* fixed target type mapping of nested objects in GraalJsExpressionHandler
* improved speed of GraalJsExpressionHandler

## 2.0.2 / 2021-09-01
* added more data types to GraalJsExpressionHandler (Date,Time etc.)
* fixed a regression bug, which was introduced in 2.0.1, when calling multiple mixins

## 2.0.1 / 2021-08-31
* improved speed of GraalJsExpressionHandler a lot
* made GraalJSExpressionHandler Thread-Safe

## 2.0.0 / 2021-08-30
* pug4j should now support all features of pugjs 2.0.4
* Made GraalJsExpressionHandler work
    * fixed scoping
    * fixed object conversion
* support bufferd code blocks
* fixed a bug when using append,prepend,replace blocks with multiple extends
* fixed text presentation of arrays and lists

## 2.0.0-beta-3 / 2021-08-13
* Implemented style attribute parsing
* Fixed class escaping. Now multiple class attributes can have different escaping 

## 2.0.0-beta-2 / 2021-08-11
* Fixed rendering of attribute values.
* Removed interpolation on attributes and filters because its also not supported in pugjs anymore

## 2.0.0-beta-1 / 2021-08-10
* replaced deprecated commons-lang3 StringEscapeUtils
* downgraded caffeine to 2.9.2 to be compatible with java 8
* ported additional code checks to lexer, to throw exception early
* some code cleanup

## 2.0.0-alpha-6 / 2021-07-23
* Implemented nested filters

## 2.0.0-alpha-5 / 2021-07-22
* Fixed argument handling for filters
* Some changes to file path handling. Replaced a NPE with an IllegalArgumentException. Some cleanup.
* Fixed text token for interpolated filter
* Fixed arguments on include filters
* Fixed pretty indents 
* Implemented whitespace sensitive tags
* Fixed linebreak issues on named blocks
* Deleted old testcases

## 2.0.0-alpha-4 / 2021-07-16
* Fixed pug4j issue #3: Fix static PUG4J API to handle new templateLoaderPath. Adding Tests.
* Fixed pug4j issue #6: Improved Error Handling to be inline with pug.js
* Improved file path resolving

## 2.0.0-alpha-3 / 2021-02-06
* Fixed pug4j issue #1: Pug4J.render() method not outputting correct HTML. (Thanks to Selaron)

## 2.0.0-alpha-2 / 2020-09-19
* Optimized Path Handling, to work again with spring integration

## 2.0.0-alpha-1 / 2020-09-18
* Upgrading Flexmark lib to latest version
* Changed compiler level to 1.8 and above
* Fixed issue #176: Upgraded to jexl 3
* Fixed issue #130: Renaming to Pug4J - All classes are updated to Pug naming.
* Default file extension is now .pug
* basePath is now templateLoaderPath
* Rewritten to be compatible with pug 2.0.4 syntax
* Fixed issue #194: Defining functions doesn't work

## 1.3.2 / 2020-03-17
* Fixed issue #193: Mixin's block argument can't execute multiple times in a loop

## 1.3.1 / 2020-02-28
* Fixed issue #191: Scoping Issue with nested loops
* Fixed issue #187: maven pom flexmark-all is too much
* Fixed issue #188: Unit tests failures on default Windows console

## 1.3.0 / 2019-10-10
* removed obsolete basePath handling. Basepath Should be configured in the FileTemplateLoader
* made file extension configurable. removed last static jade extension check.
* Fixed issue #172: json als mixin argument (quoted keys)
* Fixed issue #153: Variable assignments in for/each loops
* Improvements to issue #150: Caused by: java.lang.RuntimeException this reader only responds to

## 1.2.7 / 2017-11-02
* Improving cache syncronisation

## 1.2.6 / 2017-11-01
* Fixing issue #154: using .pug extension
* Fixing issue #157: array constructing in mixin parameters don't work
* Testcase #155: case with default not working (at least using JsExpressionHandler)
* Fixing multiline Code Blocks
* Syncronize template creation when cache is enabled

## 1.2.5 / 2016-10-24
* Fixing issue #147: Fix for issue #52 broke everything

## 1.2.4 / 2016-10-11
* Fixed issue #141: Jade4J does not support unbuffered code blocks
* Fixing issue #52: Includes in templates in paths with spaces

## 1.2.3 / 2016-06-17
* Performance improvements

## 1.2.2 / 2016-06-17
* Fixing issue #106: Filters cannot be set using xml configuration
* Testcase issue 65
* Fixing issue #78: Incorrect rendering of backslashes in attribute values
* Fixing issue #68: Multi-Line Strings include the trailing slash

## 1.2.1 / 2016-04-18
* Fixing issue #132: class attribute not supported

## 1.2.0 / 2016-04-18
* Fixing issue #135: Resource loaded using the ClasspathTemplateLoader require *.jade extension before they are copied in (Thanks to nishtahir and crowmagnumb)
* Fixing issue #129: multiple class attributes per HTML tag are not supported (breaking change in Filter interface, you need to adapt thirdparty filters)

## 1.1.4 / 2016-01-14
* set fileName property to TextNode (thx to code4craft)

## 1.1.3 / 2015-12-08
* Fixed Testcase: include-extends-of-common-template
* Added Lexer Testcases
* Updated Dependencies to newest version.

## 1.1.2 / 2015-12-02
* Fixing issue #126: Concatinate Null with String => NullPointerException
* Improved error messages

## 1.1.1 / 2015-12-02
* Fixing issue #125: NumberFormatException when comparing loop-iterator with a String

## 1.1.0 / 2015-11-29
* Feature: Add Rest Attributes to Mixins

## 1.0.10 / 2015-11-27
* Fixing issue #124: Mixin Merge not working correct
* Some Mixin refinements
* Fixing include-with-text.jade

## 1.0.9 / 2015-11-27
* Fixing jade-Output without doctype
* Fixing issue #122: Mixin Block after Mixin Call on same Line
* Fixing issue #123: Block append not working correct.

## 1.0.8 / 2015-11-26
* Fixing issue #120: Terse Mode not working as expected
* Fixed IndexOutOfBoundsException in substring.

## 1.0.7 / 2015-11-16
* Fixing issue #101: "}" symbol breaks the template inside of a string (Thanks to moio)

## 1.0.6 / 2015-11-12
* Fixing issue #118: Problems with nested/inherited attributes
* Fixing issue #117: &attributes() -> String index out of range when mixin called with ()()

## 1.0.5 / 2015-11-12
* Fixing Issue #116: &attributes() -> Classcast Exception with integers as keys in maps, inside of loops

## 1.0.4 / 2015-11-12
* Fixing Issue #115: &attributes() -> Classcast Exception with integers as keys in maps
* Fixing Issue #104: mixin definitions are ignored in extension template (Thanks to rzara)

## 1.0.3 / 2015-11-11
* Fixing Issue #114: Blank strings in brackets being casted to 0

## 1.0.2 / 2015-11-10
* Fixing Issue #113: &attributes on Mixin not working

## 1.0.1 / 2015-11-10
* Fixing Issue #112: Fixed ++ and -- recognition
* Fixing Issue #111: Maven Upgrade auf 3.2.5
* Added Testcases for closed Issues

## 1.0.0 / 2015-11-06
* In this version we updated to a lot of features of JadeJs 1.11 (2015-06-12).
* Breaking Change: Instead of 'id = 5' you must use '- var id = 5'
* Breaking Change: Instead of 'h1(attributes, class = "test")' you must use 'h1(class= "test")&attributes(attributes)'
* Breaking Change: Instead of '!!! 5' you must use 'doctype html'
* Breaking Change: Instead of '!!! 5' you must use 'doctype html'
* Jade Syntax for Conditional Comments is not supported anymore
* Thanks to rzara for contributing to issue-108

## 0.4.3 / 2015-05-27
* Accepted pull request from dusanmsk (#91) regarding mixin argument splitting and added further tests.

## 0.4.2 / 2015-03-18
* added issue89: Test files renamed (was #89 instead of #90).

## 0.4.1 / 2014-11-29
* fixed tab support #79
* .jade file extension appending is now done before the template loader #71
* added support for mixin default blocks #80

## 0.4.0 / 2013-11-20
* we are now on maven central #25
* adapted pom to meet sonatype requirements
* changed artifact group id
* fixed double output of objects implementing Map and Iterable interfaces #63

## 0.3.17 / 2013-10-09
* added sources to maven repository
* added support for multiple block statements in one mixin
* fixed issues when using if/case statements inside a mixin

## 0.3.16 / 2013-10-07
* allowed including files without having to register a specific filter
* enabled self closing tags with trailing "/" #57

## 0.3.15 / 2013-09-12
* added support for including non jade files (js, css, ...) inside a template

## 0.3.14 / 2013-08-24
* added ability to clear expression and template caches
* added new convenience method to Jade4J thats lets you use Reader #49

## 0.3.13 / 2013-08-21
* the indentation exception shows the expected indent sequence #50
* code nodes can have sub blocks #44
* better error message for invalid attribute definition #37
* blockquotes are now parsed correctly and don't interfere with "layout blocks" #45
* ExpressionStrings are now evaluated multiple times to support expressions that point to expressions #47

## 0.3.12 / 2013-06-20
* reduced jexl log level for 'unknown variable' messages
