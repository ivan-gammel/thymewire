# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Thymewire is a live template previewer for Thymeleaf with auto-reload capabilities. It serves Thymeleaf templates as web pages, loading test data models from JSON files to enable rapid template development and testing with multiple data variants.

## Build and Run Commands

### Build the project
```bash
mvn clean package
```

### Run the application
The main class is `pro.gammel.thymewire.Thymewire`. Run with:
```bash
java -cp target/thymewire-1.0-SNAPSHOT.jar pro.gammel.thymewire.Thymewire [directory]
```

### Run tests (if configured)
```bash
mvn test
```

## Architecture

### Core Components

**PreviewerServer** (`PreviewerServer.java`)
- Built on Java's `HttpServer`, handles all HTTP requests
- Routes requests through a chain of `Controller` implementations based on priority
- Controllers implement pattern: `accepts(HttpExchange)` → `respond(HttpExchange)`

**Controller Chain** (priority-based routing):
1. **TemplateController** (priority 0): Handles template rendering
   - Matches requests to templates via direct path mapping or URI template patterns
   - Loads JSON models from test resources (supports model variants via query params)
   - Applies layouts when configured via `LayoutConfiguration`
   - Handles form submissions (POST/PUT/PATCH) with redirect support
2. **IndexController**: Generates auto-index page listing all templates and model variants
3. **ResourceController**: Serves static assets (CSS, JS, images)

**SiteProvider** (`SiteProvider.java`)
- Central configuration and discovery component
- Loads `site.json` configuration (via `SiteConfigParser`)
- Discovers templates and their model variants via `TemplateLoader` and `ModelLoader`
- Maintains `LayoutConfiguration` for layout-to-template mappings
- Provides observer pattern for file change notifications (future watch mode)

**Renderer** (`Renderer.java`)
- Wraps Thymeleaf template engine
- Uses Resource4j for template and message resolution
- Configured via `TemplateEngineProvider` with custom `ComponentDialect` for component support

### Data Flow

1. **Request arrives** → `PreviewerServer.handle()`
2. **Controller selection**: Iterate controllers by priority until `accepts()` returns true
3. **TemplateController.respond()**:
   - Extract path and query params (e.g., `?__preview_model=error`)
   - Try URI template matching against `site.config().mappings()` (e.g., `/events/{id}`)
   - If matched: extract path variables, determine template and model
   - Load raw JSON model from `{test}/pages/{template}.{variant}.json`
   - Process model: deserialize objects with `class` metadata via `ClassAwareDeserializer`
   - Check for layout: `LayoutConfiguration.findByPath()` → apply layout if found
   - Render via `Renderer` with merged model
4. **Response** returned with HTML content

### Key Patterns

**Model Loading**:
- Default model: `{test}/pages/{template}.json`
- Variant models: `{test}/pages/{template}.{variant}.json`
- Layout models: `{test}/layouts/{layout}.json`
- Model structure: `{"model": {...}, "form": {...}}` where `form` defines POST behavior

**URI Template Matching** (`UriTemplateMatcher`):
- Converts patterns like `/events/{id}` to regex `^/events/([^/]+)$`
- Extracts variables from URL path (e.g., `id=123`)
- Variables can drive model selection via `Mapping.model` field

**Layout Application**:
- Layout templates wrap page templates
- Model merging: layout model as base, page model overrides
- Page template accessible via `page` variable in layout
- Layout selection via path patterns in `layouts/index.json`

**Component System** (templates package):
- `ComponentDialect`: Custom Thymeleaf dialect for reusable components
- `ComponentTemplateResolver`: Resolves component templates from resources
- `ComponentMessageResolver`: Wraps Resource4j message resolution

### Configuration

**site.json structure**:
```json
{
  "src": "src/main/resources",           // Template source folder
  "test": "src/test/resources/templates", // Test model folder
  "resources": "src/main/resources/static", // Static assets
  "model_selector": "__preview_model",     // Query param for model variants
  "mappings": [
    {
      "href": "/events/{id}",
      "templated": true,
      "template": "events/detail",
      "model": "id"  // Use {id} variable for model selection
    }
  ]
}
```

**layouts/index.json structure**:
```json
{
  "/pages/*": "default",
  "/admin/*": "admin-layout"
}
```

## Development Notes

- Java 25 project using Maven
- Key dependencies: Thymeleaf 3.1.3, Jackson 2.20.0, Resource4j 3.4.0
- HTTP server uses Sun `com.sun.net.httpserver` package
- Templates use Thymeleaf syntax and Resource4j message bundles
- Model files are standard JSON with optional `class` metadata for object instantiation
