# Running and debugging the project

The app starts in `Main.main()`. The other classes are parts of the app, so
running an individual model file such as `Driver.java` is not the same as
starting the project.

## The compile errors we reproduced

`Driver` had been changed to return `Deque<Float>`, while `DeliverySystem`
and `FileManager` still used `Deque<Integer>`. Java cannot treat those as the
same collection. That produced five compile errors before the app could start.
The conversion now covers the driver, order, backend, file loading/saving,
desktop input, terminal input, and tests. Money also uses `float` throughout.

Use `4.5f` for a float literal in Java. `4.5` is a double literal, which Java
will not implicitly narrow when a method needs a float. Collections need the
wrapper type `Float`, so a list of ratings is `Deque<Float>`.

## The VS Code setup

The current Mac had the older `georgewfraser.vscode-javac` debugger, whose
installed manifest only declares attach configurations. Oracle's Java extension
uses a different debugger type, `jdk`. The project's `type: java` launch
configurations target Microsoft's Debugger for Java instead.

The **CS233 Java** profile contains Red Hat's Java language support and
Microsoft's Java debugger. This keeps the project's Java tooling in one
profile. Other installed profiles and their extensions remain available.

1. Open the repository folder in VS Code with the CS233 Java profile.
2. Wait for Java project importing/building to finish.
3. Open Run and Debug and choose **Delivery System - Desktop** or
   **Delivery System - CLI**.
4. Put a breakpoint in `DeliverySystem.placeOrder()` or `rateDriver()` and press F5.
5. For the CLI, type into the integrated terminal. VS Code's Debug Console does
   not accept the Scanner input this app needs.

The configurations set the working directory to the repository folder, where
`Menu.txt` lives. They also use separate data folders under `data/`, so a running
desktop app does not lock the CLI debugger's data folder.

For a terminal launch on macOS, use `./run.sh` or `./run.sh --cli` from the
repository folder. On Windows, use `.\run.bat` or `.\run.bat --cli`. The scripts
compile all the source files together before launching `Main`.

## Other messages you might see

| Message | What it means and how to handle it |
| --- | --- |
| `Deque<Float> cannot be converted to Deque<Integer>` | A file still has the old integer-rating API. Use the current source files together and rebuild. |
| `Main method not found` | A model/helper class was launched. Choose the Desktop or CLI launch configuration. |
| Debugger type `java` is unsupported | Install/enable the two recommended Java extensions in the active profile. |
| `This data folder is already open` | Another process holds the data-folder lock. Close that instance or use a separate `--data-dir`. |
| `Menu.txt` cannot be found | Open the repository folder and use the supplied launch configuration or run script. |
| `release version 17 not supported` | The compiler is too old. Select a JDK 17 or newer; the current Mac has Java 21. |
| `Could not find or load main class Main` | Compilation failed or the classpath is wrong. Fix compile errors, then use the launch configuration or script. |

The first issue above was reproduced directly. The other rows explain possible
messages; they are not claims about the exact error shown in your VS Code window.

The configuration options and terminal-input behavior are documented in
[VS Code's Java debugging guide](https://code.visualstudio.com/docs/java/java-debugging).
Profiles and extension installation are documented in
[the VS Code command-line guide](https://code.visualstudio.com/docs/configure/command-line).
