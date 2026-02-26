# Architecture Decision Records (ADR) - Index

**Project:** Redmine Connector Pro v9.0  
**Location:** `docs-md/architecture/adrs/`  
**Format:** Markdown ([MADR Template](https://adr.github.io/madr/))

---

## What are ADRs?

Architecture Decision Records (ADRs) document significant architectural decisions made during the development of this project. Each ADR captures:

- **Context:** What problem needed solving
- **Decision:** What solution was chosen
- **Alternatives:** What other options were considered
- **Consequences:** Trade-offs and implications

ADRs help preserve institutional knowledge and make it easier for new team members to understand **why** the system is designed the way it is.

---

## ADR Index

| ID | Title | Status | Date | Category |
|----|-------|--------|------|----------|
| [001](ADR-001-twin-task-synchronization.md) | Twin Task Synchronization Architecture | ✅ Accepted | 2024 | Architecture |
| [002](ADR-002-async-operations-pattern.md) | Async Operations with CompletableFuture | ✅ Accepted | 2024 | Architecture / Performance |
| [003](ADR-003-centralized-logging.md) | Centralized Logging with LoggerUtil | ✅ Accepted | 2024-12 | Architecture / Code Quality |

---

## ADR Summaries

### ADR-001: Twin Task Synchronization

**Problem:** How to manage related tasks across multiple Redmine servers without database or custom fields?

**Decision:** Use pattern-based detection in task descriptions (e.g., `[Ref #{id}]`)

**Key Points:**
- Configurable patterns per client
- No external dependencies (works with vanilla Redmine)
- Visible references in plain text
- Auto-detection via search

**Impact:** ✅ Zero infrastructure, transparent to users, portable

---

### ADR-002: Async Operations

**Problem:** Network operations block UI thread, creating poor user experience

**Decision:** Adopt CompletableFuture pattern with AsyncDataService wrapper

**Key Points:**
- 75-85% faster bulk operations
- Non-blocking UI
- Parallel execution support
- Clean functional composition

**Impact:** ✅ Dramatically improved responsiveness, better UX

---

### ADR-003: Centralized Logging

**Problem:** Inconsistent logging (System.out, printStackTrace) makes debugging difficult

**Decision:** Custom LoggerUtil with simple, consistent API

**Key Points:**
- No external dependencies
- Structured format with timestamps
- Log levels (INFO, WARNING, ERROR, DEBUG)
- Replaced 17 printStackTrace() instances

**Impact:** ✅ Consistent logging, better traceability, easier debugging

---

##  ADR Lifecycle

### Statuses

- **Proposed:** Under discussion
- **Accepted:** Decision made and implemented
- **Deprecated:** No longer recommended
- **Superseded:** Replaced by newer ADR

### When to Create an ADR

Create an ADR when making decisions about:

- **Architecture patterns** (layering, async/sync, data flow)
- **Technology choices** (libraries, frameworks, tools)
- **Major refactoring** (breaking changes, new patterns)
- **Performance optimizations** (caching, lazy loading)
- **Security decisions** (encryption, authentication)

### ADR Template

When creating a new ADR, include:

1. **Title:** Clear, descriptive name
2. **Status:** Proposed/Accepted/Deprecated/Superseded
3. **Date:** When decision was made
4. **Context:** Problem statement and background
5. **Decision Drivers:** Factors influencing the decision
6. **Considered Options:** Alternative approaches
7. **Decision Outcome:** Chosen solution with rationale
8. **Consequences:** Positive, negative, and neutral impacts
9. **Implementation Details:** How it was built
10. **Validation:** How success is measured
11. **Related Decisions:** Links to other ADRs
12. **References:** External resources, discussions

---

## Related Documentation

- [API Documentation](../../API_DOCUMENTATION.md) - Comprehensive API guide
- [DataService.java](../../../src/main/java/redmineconnector/service/DataService.java) - Javadoc reference
- [Improvement Plan](../../IMPROVEMENT_PLAN.md) - Overall quality improvements roadmap
- [Walkthrough](../../../.gemini/antigravity/brain/.../walkthrough.md) - Implementation details

---

## Contributing New ADRs

To add a new ADR:

1. **Create file:** `ADR-XXX-kebab-case-title.md` (next number in sequence)
2. **Follow template:** Use existing ADRs as reference
3. **Update this index:** Add entry to table and summaries
4. **Cross-link:** Reference in related ADRs, code, and documentation
5. **Review:** Have team review before marking as "Accepted"

---

## Questions?

For questions about these architectural decisions:

- Review the full ADR document for detailed rationale
- Check related code in referenced files
- Consult API documentation for usage examples
- Reach out to the development team

---

**Last Updated:** December 29, 2025  
**Maintainer:** Redmine Connector Team
