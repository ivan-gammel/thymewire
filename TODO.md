# Thymewire TODO

This file tracks features mentioned in documentation but not yet implemented, as well as planned enhancements.

## Missing Features from README

### 1. Auto-reload / Live Preview (HIGH PRIORITY)
**Status:** Not implemented
**README claims:** "live template previewer for Thymeleaf with auto-reload of changes"

**Implementation plan:**
- [ ] Implement file watching using Java WatchService API
- [ ] Complete the `SiteProvider.fileUpdated(Path)` method (currently empty)
- [ ] Wire up listener infrastructure that already exists
- [ ] Add `--watch` command-line flag
- [ ] Detect changes to:
  - [ ] Template files (*.html)
  - [ ] Model files (*.json)
  - [ ] Configuration files (site.json)
  - [ ] Layout configuration
- [ ] Trigger browser refresh (WebSocket or Server-Sent Events)
- [ ] Add debouncing to prevent excessive reloads

**Files to modify:**
- `src/main/java/pro/gammel/thymewire/core/SiteProvider.java` (line 119-120)
- `src/main/java/pro/gammel/thymewire/Thymewire.java` (add --watch flag)
- Create new `FileWatcher` class

### 2. Static Site Generation
**Status:** Not implemented
**README mentions:** "thw generate - generates the static site"

**Implementation plan:**
- [ ] Create `StaticSiteGenerator` class
- [ ] Add `generate` command support to main class
- [ ] Iterate through all templates and model variants
- [ ] Render each combination to static HTML files
- [ ] Copy static resources (CSS, JS, images)
- [ ] Support `--target` flag to specify output directory (default: `./target/www`)
- [ ] Generate proper directory structure matching URLs
- [ ] Handle layouts properly in static generation

**Files to create:**
- `src/main/java/pro/gammel/thymewire/core/StaticSiteGenerator.java`

**Files to modify:**
- `src/main/java/pro/gammel/thymewire/Thymewire.java` (add command parsing)

### 3. CLI Wrapper Script
**Status:** Not implemented
**README shows:** `thw serve`, `thw generate`

**Implementation plan:**
- [ ] Create `thw` shell script (Unix/Linux/macOS)
- [ ] Create `thw.bat` batch file (Windows)
- [ ] Support commands:
  - [ ] `thw serve [options]` - Start preview server
  - [ ] `thw generate [options]` - Generate static site
- [ ] Pass through options: `--port`, `--dir`, `--watch`, `--target`, `--site`
- [ ] Handle JAR location detection
- [ ] Add help text (`thw --help`)

**Files to create:**
- `thw` (shell script)
- `thw.bat` (Windows batch file)

## Code Quality Improvements

### 4. Complete File Watcher Infrastructure
**Status:** Partially implemented
**Current state:** Listener interface exists but unused

**Tasks:**
- [ ] Complete `fileUpdated()` implementation in SiteProvider
- [ ] Notify registered listeners on file changes
- [ ] Test listener subscription/unsubscription
- [ ] Add granular update types (config, template, model, layout)

### 5. Test Coverage
**Status:** No tests exist

**Tasks:**
- [ ] Add JUnit 5 and Mockito dependencies
- [ ] Unit tests for UriTemplateMatcher
- [ ] Unit tests for ModelLoader and TemplateLoader
- [ ] Unit tests for ClassAwareDeserializer
- [ ] Integration tests for TemplateController
- [ ] Test path traversal protection
- [ ] Test URL decoding
- [ ] Test form submission handling

### 6. Error Handling Improvements
**Tasks:**
- [ ] Create custom exception hierarchy (ThymewireException)
- [ ] Add proper error pages with development/production modes
- [ ] Better error messages for missing templates/models
- [ ] Validate site.json on startup with helpful error messages

### 7. Documentation
**Tasks:**
- [ ] Add JavaDoc to public APIs
- [ ] Document site.json schema with examples
- [ ] Add examples directory with sample projects
- [ ] Document layout configuration format
- [ ] Document component system usage
- [ ] Add troubleshooting guide

## Future Enhancements

### 8. Development Features
- [ ] Hot reload without browser refresh (inject JavaScript)
- [ ] Error overlay in browser when compilation fails
- [ ] Template compilation error reporting with line numbers
- [ ] Model validation against JSON schema
- [ ] Template performance profiling

### 9. Advanced Features
- [ ] Support for other template engines (Freemarker, Mustache)
- [ ] REST API for programmatic access
- [ ] Template preview in different viewports (responsive testing)
- [ ] Screenshot generation for templates
- [ ] Visual regression testing integration
- [ ] Support for partial/fragment rendering

### 10. Build/Distribution
- [ ] Create executable JAR with dependencies
- [ ] Add Maven plugin for integration
- [ ] Gradle plugin
- [ ] Docker image
- [ ] Homebrew formula (macOS)
- [ ] Publish to Maven Central

## Migration Notes

### Completed Refactoring (2026-01-11)
✅ Package structure reorganized:
- `server/` - HTTP server layer
- `core/` - Business logic
- `rendering/thymeleaf/` - Thymeleaf integration
- `rendering/layout/` - Layout system
- `rendering/` - Deserialization utilities

✅ Security fixes:
- Path traversal protection
- URL decoding for query parameters
- Input validation

✅ Code consistency:
- Logger declarations standardized
- Magic strings eliminated (DEFAULT_MODEL_SELECTOR constant)
- Configurable port and directory via command-line
- Expanded MIME type support
