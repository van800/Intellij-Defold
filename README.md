# Defold IntelliJ Plugin

Build Defold games directly inside IntelliJ IDEA. This plugin focuses on the authoring and debugging workflows so you can write Lua, navigate code, and launch the game engine without leaving the IDE.

## Features

- **📁 Project awareness** – Detects Defold workspaces and keeps paths in sync.
- **🧠 Smart Lua editing** – EmmyLua2 + LSP4IJ provide completion, annotations, syntax highlighting, linting, navigation, refactors, and other editor goodies for your Lua scripts.
- **📄 Script templates** – Create scripts from IntelliJ with the expected boilerplate.
- **🪲 Debugger** – Full mobdebug experience without starting the Defold editor: breakpoints, run-to-cursor, expression evaluation, watches, inline values, call stacks, and coroutine support.
- **🚀 Build + Run + Debug** – Trigger clean, build, run, and debug from IntelliJ with automatic engine launching and port management.
- **🔥 Hot reloading** – Reflect changes to Lua scripts with a simple hotkey.
- **🖥️ Multi-platform** – Works on Windows, macOS, (Linux untested yet).

## Requirements

- IntelliJ IDEA 2025.2 or newer
- Java 17+ (JDK or JRE)

## Usage

No manual setup is required. Just open your Defold project through `File | Open`, and you're good to go 🖖 

The plugin auto-detects the project, configures toolchains, and generates the `.luarc.json` file needed for LuaLS. 

### Notes

- This plugin is primarily focused on coding and debugging. Keep using the official Defold editor for the rest of your workflow.
- You do not need to sprinkle `mobdebug` snippets or maintain annotation files. The plugin manages LuaLS configuration and debugger scripts for you—just leave the generated `.luarc.json` in place.

## Dependencies

The plugin depends on other open-source IntelliJ plugins to deliver Lua editing, language server support, and asset helpers. They are downloaded automatically when you install the Defold plugin:

- [EmmyLua2][emmylua] – Lua language support, annotations, syntax highlighting, code completion, and more.
- [LSP4IJ][lsp4ij] – Language Server Protocol client.
- [OpenGL Plugin][opengl] – shader syntax highlighting used by Defold resources.
- [INI4Idea][ini4idea] – `.ini` editing utilities for `game.project`.
- [Defold Annotations][annotations] – LuaLS-compatible annotations for Defold API.

[emmylua]: https://github.com/EmmyLua/Intellij-EmmyLua2
[lsp4ij]: https://github.com/redhat-developer/lsp4ij
[opengl]: https://github.com/nanchengyin/OpenGL-Plugin
[ini4idea]: https://github.com/JetBrains/intellij-community/tree/master/plugins/ini4idea
[annotations]: https://github.com/astrochili/defold-annotations
