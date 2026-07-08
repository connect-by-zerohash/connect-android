# Code Review Policy - ConnectSDK Android

## Overview

All code changes must undergo peer review before merging to ensure quality, security, and consistency.

---

## Requirements

- **All PRs require at least 1 approval**

---

## Before Submitting PR

1. Review your own diff
2. Run tests locally
3. Check for compiler warnings
4. Follow [Code Standards](CODE_STANDARDS.md)
5. Update documentation

---

## Review Focus

Reviewers check:

- **Correctness:** Does it work? Edge cases handled?
- **Standards:** Follows [Code Standards](CODE_STANDARDS.md)
- **Security:** No hardcoded secrets, proper validation
- **Testing:** Adequate coverage
- **Maintainability:** Readable and well-structured

---

**Examples:**
```
[BLOCKING] This could cause a memory leak - listener should be nullable
[SUGGESTION] Consider using a sealed class here instead of enum
[QUESTION] Why use !! instead of safe call?
[PRAISE] Great use of extension functions!
```

---

## Review Checklist

- [ ] Naming follows conventions (PascalCase/camelCase)
- [ ] Access modifiers explicit
- [ ] Proper null safety (avoid !!)
- [ ] Public APIs documented with KDoc
- [ ] No sensitive data logged
- [ ] No hardcoded credentials
- [ ] Tests cover new functionality
- [ ] No compiler warnings
- [ ] No lint errors

**Full standards:** See [Code Standards](CODE_STANDARDS.md)


## Resources

- [Code Standards](CODE_STANDARDS.md) - Coding conventions
- [PR Template](.github/PULL_REQUEST_TEMPLATE.md) - PR checklist
