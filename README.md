# Persistence Layer Architecture (JPA + Hibernate + QueryDSL)

This project implements a **deterministic, pagination-safe, and fetch-graph-safe persistence architecture** on top of **Spring Data JPA + Hibernate + QueryDSL**.

It is designed to handle:

- Complex nested filters  
- Multi-column sorting  
- Large datasets  
- Collection fetch graphs  
- Stable pagination without data corruption  

---

## 📌 Core Principles

### 1. Deterministic Pagination (No Data Corruption)
Pagination **must always return the correct global ordering** for a given:

```
WHERE + ORDER BY + OFFSET + LIMIT
```

This repository **does not rely on JOIN-based pagination**, which is known to cause:
- Duplicate rows
- Missing records
- Incorrect page boundaries
- Hibernate in-memory paging warnings

---

### 2. EXISTS-Based Nested Filtering (No JOIN Filters)
All nested criteria are implemented using **`EXISTS` subqueries**, never JOINs.

✅ Correct  
```sql
WHERE EXISTS (
  SELECT 1 FROM role_action ra
  JOIN action a ON a.id = ra.action_id
  WHERE ra.role_id = role.id
    AND a.page = 'User'
)
```

❌ Forbidden  
```sql
LEFT JOIN role_action ra ...
WHERE a.page = 'User'
```

This guarantees:
- Correct counts
- Stable paging
- No accidental row multiplication

---

## 🧠 Query Execution Strategy

### Fast Path (No Collection Fetch)
Used when:
- No collection fetch graphs are requested
- Only `@ManyToOne` / `@OneToOne` relations are fetched

Flow:
```
SELECT entity
FROM table
WHERE filter
ORDER BY sort
OFFSET / LIMIT
```

---

### ID-First Paging Path (Collection Fetch Safe)
Used when:
- Collection fetch graphs are requested
- Paging + sorting is required

#### Phase 1 — ID Selection
```
SELECT id
FROM table
WHERE filter
ORDER BY sort
OFFSET / LIMIT
```

Defines **exact page membership**.

#### Phase 2 — Entity Fetch
```
SELECT entity
FROM table
LEFT JOIN collections
WHERE id IN (:ids)
ORDER BY <strategy>
```

Ensures:
- No Hibernate paging warnings
- No incorrect result sets
- No duplicated entities

---

## 🐘 PostgreSQL Optimization

```java
public static final boolean IS_POSTGRES_DB = true;
```

### Why this flag exists
- PostgreSQL guarantees **deterministic ORDER BY behavior**
- Allows safe reapplication of `ORDER BY` in Phase 2
- Avoids SQL hacks like `CASE` or `array_position`

### Behavior
| Database | Phase-2 Ordering |
|--------|------------------|
| PostgreSQL | Re-apply `ORDER BY` |
| Other DBs | Preserve ID order via CASE expression |

> ⚠️ This flag is **explicit by design** — no auto-detection magic.

---

## 🚫 API Contract Rules

### `findAll()`
- ❌ Paging NOT allowed
- ✅ Sorting allowed
- Used only for full result sets

```java
findAll(criteria);              // OK
findAll(criteria with paging);  // ❌ throws exception
```

---

### `findByPaging()`
- ✅ The **only** paging entry point
- Supports:
  - Offset + limit
  - Page number
  - Multi-column sorting
  - Nested criteria
  - Fetch graphs

```java
findByPaging(criteria, "Role(roleActions(action))");
```

---

## 🔀 Sorting Rules

- Supports **multi-column sorting**
- Sorting is always applied:
  - Before paging (defines page membership)
  - After entity fetch (when required)
- Invalid sort properties fail fast with clear errors

Example:
```java
criteria.addSort("roleType", Sort.Direction.ASC);
criteria.addSort("id", Sort.Direction.DESC);
```

---

## 📊 Counting Strategy

- `count()` queries:
  - Never use JOINs
  - Use the same filter logic
  - Always reflect true dataset size

Guarantees:
- `recordsTotal` is correct
- No mismatch between content and total

---

## 🧪 Test Coverage

The architecture is validated with integration tests covering:

- Nested criteria with EXISTS
- Multi-column sorting
- Offset + limit paging
- Fetch graphs with collections
- Deterministic ordering across pages

Example test:
```java
findByPaging_withNestedCriteria_multiSort_andOffsetLimit()
```

---

## 🛡️ What This Design Prevents

- ❌ Hibernate in-memory paging
- ❌ Duplicate entities
- ❌ Missing records
- ❌ Broken pagination with joins
- ❌ Incorrect total counts
- ❌ Non-deterministic ordering

---

## 📐 Design Philosophy

This persistence layer favors:

- **Correctness over convenience**
- **Explicit behavior over magic**
- **Predictable SQL over ORM guesswork**
- **Long-term stability over short-term shortcuts**

It is suitable for:
- Large datasets
- Financial systems
- Admin panels
- Audited / regulated environments

---

## 🏁 Final Notes

This is not boilerplate CRUD.

This is a **carefully designed persistence foundation** intended to scale safely as:
- New entities are added
- New nested criteria are introduced
- Dataset size grows

