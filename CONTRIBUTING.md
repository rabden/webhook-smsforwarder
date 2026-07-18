# Contributing to SMS Forwarder

Thank you for your interest in contributing to SMS Forwarder! This document provides guidelines and instructions for contributing to the project.

## 🤝 How to Contribute

### Reporting Bugs

Before creating bug reports, please check the existing issues to avoid duplicates. When creating a bug report, include:

- **Title**: A clear and descriptive title
- **Description**: A detailed description of the issue
- **Steps to Reproduce**: Steps to reproduce the behavior
- **Expected Behavior**: What you expected to happen
- **Actual Behavior**: What actually happened
- **Environment**: 
  - Device model
  - Android version
  - App version
- **Screenshots**: If applicable, include screenshots
- **Logs**: Relevant logcat output

### Suggesting Enhancements

Enhancement suggestions are welcome! Please include:

- **Title**: A clear and descriptive title
- **Description**: A detailed description of the proposed enhancement
- **Motivation**: Why this enhancement would be useful
- **Alternatives**: Alternative solutions or features you've considered
- **Additional Context**: Any other context or screenshots

## 🛠️ Development Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK API 36
- Git

### Setting Up the Development Environment

1. **Fork the repository**
   ```bash
   # Fork the repository on GitHub
   # Then clone your fork
   git clone https://github.com/YOUR_USERNAME/webhook-smsforwarder.git
   cd webhook-smsforwarder
   ```

2. **Add upstream remote**
   ```bash
   git remote add upstream https://github.com/rabden/webhook-smsforwarder.git
   ```

3. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

4. **Sync Gradle**
   - Android Studio will automatically sync Gradle
   - Wait for the sync to complete

5. **Run the app**
   - Connect an Android device or start an emulator
   - Click the "Run" button or press Shift+F10

## 📋 Coding Standards

### Kotlin Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Keep functions small and focused
- Add comments for complex logic
- Use data classes for data holders

### Compose UI

- Follow Compose best practices
- Use composables for reusable UI components
- Keep composables stateless when possible
- Use Material3 components
- Follow accessibility guidelines

### Architecture

- Follow MVVM pattern
- Use repositories for data access
- Keep ViewModels focused on UI logic
- Use coroutines for asynchronous operations
- Follow dependency injection principles

### Code Formatting

- Use the official Kotlin code style (configured in `gradle.properties`)
- Format code before committing (Cmd+Option+L on Mac, Ctrl+Alt+L on Windows/Linux)
- Remove unused imports
- Keep line length under 120 characters

## 🧪 Testing

### Unit Tests

- Write unit tests for business logic
- Test ViewModel functions
- Mock dependencies using appropriate frameworks
- Aim for high test coverage

```kotlin
class ContactsViewModelTest {
    @Test
    fun `addContact should add number to repository`() {
        // Test implementation
    }
}
```

### Instrumented Tests

- Write instrumented tests for UI components
- Test Compose UI interactions
- Test database operations

```kotlin
class ContactsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `when add button clicked, dialog should appear`() {
        // Test implementation
    }
}
```

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# Specific test class
./gradlew test --tests ContactsViewModelTest
```

## 📝 Commit Guidelines

### Commit Message Format

Follow conventional commits:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### Examples

```
feat(settings): add custom header support
fix(webhook): retry failed webhook requests
docs(readme): update installation instructions
refactor(worker): simplify worker chain logic
```

## 🔄 Pull Request Process

1. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
   - Implement your feature or fix
   - Add/update tests
   - Update documentation if needed
   - Follow coding standards

3. **Commit your changes**
   ```bash
   git add .
   git commit -m "feat: add your feature description"
   ```

4. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

5. **Create a Pull Request**
   - Go to the original repository on GitHub
   - Click "New Pull Request"
   - Select your branch
   - Fill in the PR template
   - Link related issues
   - Request review from maintainers

### Pull Request Checklist

- [ ] Code follows project style guidelines
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] Commit messages are clear
- [ ] No merge conflicts
- [ ] Builds and tests pass successfully

## 🎯 Areas for Contribution

We welcome contributions in the following areas:

### Features
- Additional webhook authentication methods
- Message content filtering
- Multiple webhook endpoint support
- Message scheduling
- Configuration export/import
- Integration with third-party services

### UI/UX
- Dark theme improvements
- Additional language translations
- Accessibility enhancements
- Animations and transitions
- Custom themes

### Documentation
- Tutorial improvements
- API documentation
- Architecture diagrams
- Video tutorials
- Troubleshooting guides

### Testing
- Additional unit tests
- UI component tests
- Integration tests
- Performance tests

## 📧 Getting Help

If you need help with contributing:

- Read the documentation in the repository
- Check existing issues and PRs at [GitHub Issues](https://github.com/rabden/webhook-smsforwarder/issues)
- Ask questions in issues (with the "question" label)
- Contact maintainers: ringlabs3@gmail.com

## 🌟 Recognition

Contributors will be recognized in:
- Contributors section in README
- Release notes for significant contributions
- Hall of Fame (to be implemented)

## 📄 License

By contributing to SMS Forwarder, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to SMS Forwarder! Your contributions help make this project better for everyone.