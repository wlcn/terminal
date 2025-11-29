# Terminal Application Development Environment Setup

This guide provides step-by-step instructions for setting up a development environment for the terminal application, covering WSL installation, JDK 21, Rust environment, and debugging configuration.

## Table of Contents
1. [WSL Installation](#wsl-installation)
2. [JDK 21 Installation in WSL](#jdk-21-installation-in-wsl)
3. [Gradle Installation in WSL](#gradle-installation-in-wsl)
4. [Rust Environment Setup in WSL](#rust-environment-setup-in-wsl)
5. [Project Configuration](#project-configuration)
   - [Kotlin Project (kt-terminal)](#kotlin-project-kt-terminal)
   - [Rust Project (rs_terminal)](#rust-project-rs-terminal)
6. [Debugging Setup](#debugging-setup)
   - [IntelliJ IDEA (Recommended for Kotlin)](#intellij-idea-recommended-for-kotlin)
   - [VS Code with Remote - WSL Extension](#vs-code-with-remote---wsl-extension)
7. [Running the Applications](#running-the-applications)
8. [Verification](#verification)

## 1. WSL Installation

### Step 1: Enable WSL in Windows
Open PowerShell as Administrator and run:

```powershell
wsl --install
```

This command:
- Enables the required Windows features
- Downloads and installs Ubuntu (default distribution)
- Sets up WSL 2 as the default

### Step 2: Configure WSL
After installation, Ubuntu will launch automatically. Set up your Linux username and password when prompted.

### Step 3: Update WSL Packages
Run the following commands in the Ubuntu terminal to update packages:

```bash
sudo apt update
sudo apt upgrade -y
```

## 2. JDK 21 Installation in WSL

### Step 1: Install JDK 21
Run the following commands in the Ubuntu terminal:

```bash
sudo apt install openjdk-21-jdk -y
```

### Step 2: Verify JDK Installation

```bash
java -version
javac -version
```

Expected output:
```
openjdk version "21.0.2" 2024-01-16
OpenJDK Runtime Environment (build 21.0.2+13-Ubuntu-1ubuntu3)
OpenJDK 64-Bit Server VM (build 21.0.2+13-Ubuntu-1ubuntu3, mixed mode, sharing)
```

## 3. Gradle Installation in WSL

### Option 1: Install via APT (Easy, but may not be latest version)

```bash
sudo apt install gradle -y
```

Verify installation:
```bash
gradle --version
```

### Option 2: Install Latest Version Manually (Recommended)

```bash
# Download latest Gradle (check https://gradle.org/releases/ for latest version)
VERSION=8.7
wget https://services.gradle.org/distributions/gradle-${VERSION}-bin.zip -P /tmp

# Extract to /opt
sudo unzip -d /opt/gradle /tmp/gradle-${VERSION}-bin.zip

# Create symbolic link
sudo ln -s /opt/gradle/gradle-${VERSION} /opt/gradle/latest

# Add to PATH
echo "export GRADLE_HOME=/opt/gradle/latest" >> ~/.bashrc
echo "export PATH=\$GRADLE_HOME/bin:\$PATH" >> ~/.bashrc
source ~/.bashrc
```

Verify installation:
```bash
gradle --version
```

### Option 3: Install via SDKMAN (For managing multiple versions)

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash

# Activate SDKMAN
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install latest Gradle
sdk install gradle
```

## 4. Rust Environment Setup in WSL

### Step 1: Install Rust Toolchain
Run the following command in the Ubuntu terminal:

```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
```

Follow the prompts to complete the installation. When asked, select the default installation option.

### Step 2: Add Rust to PATH
Restart your terminal or run:

```bash
source $HOME/.cargo/env
```

### Step 3: Verify Rust Installation

```bash
rustc --version
cargo --version
```

Expected output:
```
rustc 1.75.0 (82e1608df 2023-12-21)
cargo 1.75.0 (1d8b05cdd 2023-11-20)
```

### Step 4: Install Additional Dependencies

```bash
sudo apt install libtinfo5 libncurses5-dev -y
```

## 4. Project Configuration

### Kotlin Project (kt-terminal)

#### Update build.gradle.kts for JDK 21
Open `kt-terminal/build.gradle.kts` and ensure it uses JDK 21:

```kotlin
plugins {
    kotlin("jvm") version "1.9.22"
    // other plugins
}

kotlin {
    jvmToolchain(21)  // Set JDK version for Kotlin compilation
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}
```

### Rust Project (rs_terminal)

The Rust project should work with the default Cargo configuration. No additional changes are needed for JDK 21 compatibility.

## 5. Debugging Setup

### IntelliJ IDEA (Recommended for Kotlin)

#### Step 1: Enable WSL Integration
1. Open IntelliJ IDEA
2. Go to `File` → `Settings` → `Build, Execution, Deployment` → `WSL`
3. Select your WSL distribution (Ubuntu)
4. Set the JDK path to WSL's JDK 21 (usually `/usr/lib/jvm/java-21-openjdk-amd64`)

#### Step 2: Import Project from WSL
1. Go to `File` → `Open`
2. Navigate to `\\wsl$\Ubuntu\` (replace with your WSL distribution name if different)
3. Select your project directory (e.g., `\\wsl$\Ubuntu\home\yourusername\github\wlcn\terminal`)
4. Click `OK` to open the project

#### Step 3: Debug Configuration
1. Set breakpoints in your Kotlin code
2. Go to `Run` → `Debug 'Application'` (or your main class)
3. IntelliJ will handle WSL debugging seamlessly

### VS Code with Remote - WSL Extension

#### Step 1: Install Required Extensions
1. Open VS Code
2. Install the following extensions:
   - `Remote - WSL` by Microsoft
   - `Java Extension Pack` by Microsoft (for Kotlin/Java debugging)
   - `Rust Analyzer` by rust-lang (for Rust development)

#### Step 2: Open Project in WSL
1. Press `F1` to open the command palette
2. Type `WSL: Open Folder in WSL` and select your project directory
3. VS Code will connect to WSL and open the project

#### Step 3: Debug Configuration for Kotlin
1. Create a `launch.json` file in the `.vscode` directory:
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "java",
         "name": "Debug (WSL)",
         "request": "launch",
         "mainClass": "org.now.terminal.Application",
         "projectName": "kt-terminal",
         "vmArgs": "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
       }
     ]
   }
   ```

2. Press `F5` to start debugging
3. VS Code will connect to WSL's JVM and hit your breakpoints

#### Step 4: Debug Configuration for Rust
1. Create a `launch.json` file in the `.vscode` directory:
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "lldb",
         "request": "launch",
         "name": "Debug Rust Terminal",
         "program": "${workspaceFolder}/rs_terminal/target/debug/rs_terminal",
         "args": [],
         "cwd": "${workspaceFolder}/rs_terminal",
         "preLaunchTask": "cargo build",
         "sourceLanguages": ["rust"]
       }
     ]
   }
   ```

2. Press `F5` to start debugging the Rust application

## 6. Running the Applications

### Run Kotlin Terminal Application

#### From WSL Terminal

```bash
cd /path/to/terminal/kt-terminal
./gradlew run
```

#### From IntelliJ IDEA
1. Set the run configuration to `Application` with the main class `org.now.terminal.Application`
2. Click the `Run` button (green triangle)

### Run Rust Terminal Application

#### From WSL Terminal

```bash
cd /path/to/terminal/rs_terminal
cargo run
```

#### From VS Code
1. Set the run configuration to `Debug Rust Terminal`
2. Click the `Run` button (green triangle)

## 7. Verification

### Verify Kotlin Project Build

```bash
cd /path/to/terminal/kt-terminal
./gradlew compileKotlin
```

Expected output:
```
BUILD SUCCESSFUL in Xs
1 actionable task: 1 executed
```

### Verify Rust Project Build

```bash
cd /path/to/terminal/rs_terminal
cargo build
```

Expected output:
```
   Compiling rs_terminal v0.1.0 (/path/to/terminal/rs_terminal)
    Finished dev [unoptimized + debuginfo] target(s) in X.XXs
```

### Verify JDK Version in Project

```bash
cd /path/to/terminal/kt-terminal
./gradlew -version
```

Check that the output shows JDK 21:
```
JVM:          21.0.2 (Ubuntu 21.0.2+13-Ubuntu-1ubuntu3)
```

## Troubleshooting

### WSL Not Starting
- Ensure virtualization is enabled in your BIOS
- Run `wsl --list --verbose` to check WSL status
- Try resetting WSL with `wsl --shutdown`

### JDK Version Mismatch
- Verify JDK 21 is the default with `update-alternatives --config java`
- Check that IntelliJ/VS Code are configured to use the correct JDK path

### Rust Build Errors
- Ensure all dependencies are installed: `sudo apt install build-essential`
- Update Rust with `rustup update`

### Debugging Issues
- Ensure WSL integration is properly configured in your IDE
- Check that firewalls aren't blocking the debug port (default: 5005)
- Verify that the project is opened from the WSL filesystem (not Windows filesystem)

## Conclusion

You now have a fully configured development environment for the terminal application with WSL, JDK 21, Rust, and debugging support. This setup provides a consistent development experience across Windows and Linux, leveraging the performance benefits of WSL while maintaining the convenience of Windows IDEs.

For more information on WSL development, refer to the [Microsoft WSL Documentation](https://learn.microsoft.com/en-us/windows/wsl/).
