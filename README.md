Thymewire v1.0
==============

## Overview

Thymewire is a live template previewer for Thymeleaf with auto-reload of changes, user-defined variants of data model 
for each template and support of form submissions.


## How does it work?

Thymewire picks up your Thymeleaf templates from designated folder and serves them as web pages. To render the template 
correctly, Thymewire loads a test model file in JSON format from a separate source folder. The model file may exist 
in multiple variants to test branching in the template. Layouts are supported: site configuration file may describe, 
which layouts are used for which templates (pattern matching is supported).

### Examples
Given the following content in the file system:
```
   /src
     /main
       /resources
          /templates
             /start
               /login.html
     /test
       /resources
          /templates
             /start
               /login.json
               /login.error.json
   site.json
```
1. The Thymewire server will respond to HTTP GET `/start/login.html` with the template rendered using model `/start/login.json`.
2. The request for `/start/login.html?__preview_model=error` will receive the template rendered using model `login.error.json` in response.
3. Server will automatically generate list of links to the available templates and their variants with different models when serving `/`
4. `site.json` offers advanced configuration options including URL mappings, layout configuration and location of template folders.

## Usage

Run preview in the current folder on default port (8085):
```bash
java -cp thymewire.jar pro.gammel.thymewire.Thymewire
```

Run preview on a specific port:
```bash
java -cp thymewire.jar pro.gammel.thymewire.Thymewire --port 9090
```

Run preview from a specific directory:
```bash
java -cp thymewire.jar pro.gammel.thymewire.Thymewire --dir /path/to/project
```

### Command line syntax

```bash
java -cp thymewire.jar pro.gammel.thymewire.Thymewire [options]
```

Supported command-line options:

 * `--port <port>` - the port on which server must start (default: 8085)
 * `--dir <path>` - the project directory to serve (default: current directory)

### Model Variants

Templates can have multiple model variants for testing different data scenarios. Use the `__preview_model` query parameter (or custom parameter configured in `site.json`) to select a specific model variant:

```
http://localhost:8085/start/login.html?__preview_model=error
```

This will render the template using `login.error.json` instead of the default `login.json`.

### Security

Thymewire includes security features to protect your file system:

* **Path traversal protection**: Resource paths are validated to prevent access outside the configured resources directory
* **Input validation**: Query parameters are properly URL-decoded and validated
* **Safe file serving**: Only files within the designated template and resource directories are accessible


