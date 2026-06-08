# Hetero — Task Management System
### Data Structure Implementation and Performance Analysis

---

## Project Overview

Hetero is a desktop task management application built with Java and JavaFX. Beyond being a functional to-do app, it is designed as a hands-on study of how different data structures behave under the same workload.

The user can switch between three data structure implementations at runtime — HashMap, LinkedList, and ArrayList — and the application benchmarks every operation in nanoseconds so the performance difference is directly observable.

The project was built as part of our Object-Oriented Programming and Data Structures coursework.

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| UI Framework | JavaFX 21 with FXML |
| Database | SQLite via JDBC (xerial 3.45.1) |
| Build Tool | Apache Maven 3.9 |
| Design Pattern | Strategy Pattern |

---

## Implementation Details

### Data Structures

The project implements three distinct in-memory stores, all behind a single `TaskRepository` interface. This is the **Strategy Pattern** — the rest of the application only talks to the interface, so any of the three implementations can be swapped in at runtime without changing any other code.

#### HashMap (`HashMapTaskRepo`)
- Stores tasks in a `HashMap<Integer, Task>` keyed by task id
- Space Complexity: O(n) where n is the number of tasks
- Best for scenarios where fast individual task lookups are the priority
- Insert, lookup, and delete all run in **O(1) average time** using the hash function

#### LinkedList (`LinkedListTaskRepo`)
- Stores tasks in a `LinkedList<Task>` as a chain of nodes
- Space Complexity: O(n) with per-node pointer overhead
- Appending to the tail is **O(1)** — only two pointers are updated
- Finding or deleting by id requires scanning every node — **O(n)**
- Uses a `ListIterator` for in-place updates without remove/re-insert

#### ArrayList (`ArrayListTaskRepo`)
- Stores tasks in an `ArrayList<Task>` backed by a resizable array
- Space Complexity: O(n) in contiguous memory
- Appending is **O(1) amortised** — occasionally O(n) when the array resizes
- Delete is the most expensive: O(n) scan plus O(n) element shift to close the gap
- Most familiar structure but pays a memory compaction cost on every delete

---

### Core Features

#### Task Management
- Add, edit, and delete tasks
- Search tasks by title or category (live filter as you type)
- Filter by status (Pending / Completed) or priority level
- Toggle task completion with an inline checkbox

#### Task Fields

| Field | Type | Notes |
|---|---|---|
| id | int | Auto-assigned by SQLite |
| title | String | Required |
| notes | String | Optional |
| priority | Enum | CRITICAL / HIGH / MEDIUM / LOW / MINIMAL |
| category | String | Defaults to "General" |
| dueDate | LocalDate | Optional |
| isCompleted | boolean | Defaults to false |

#### User Authentication
- Login and registration with form validation
- Default account: `admin` / `admin`
- Session managed by `SessionManager` for the lifetime of the app

---

### Benchmarking

Every operation on the in-memory collection is timed using `System.nanoTime()` before and after the collection call. The SQLite write-through sync happens **after** the timer stops so it never inflates the measurement.

**Live benchmark output (topbar):**
```
HashMap  ·  12 tasks  ·  1,204,300 ns
```

**Console output per operation:**
```
[Benchmark] HashMap Insert: 4,200 ns
[Benchmark] LinkedList FindById: 38,900 ns
[Benchmark] ArrayList Delete: 12,100 ns
```

#### Performance Test Tool (`PerformanceTest.java`)

A separate interactive terminal tool that runs a full benchmark suite. It first performs a JVM warmup phase (500 operations per structure) to let the JIT compiler optimise the hotspots before measuring, making the results more realistic.

```
==================================================
     COMPLEXITY ANALYSER & BENCHMARK TOOL
==================================================
Select a Data Structure to evaluate:
1. ArrayList
2. LinkedList
3. HashMap
4. Run All Structures (Side-by-Side Comparison)
5. Exit
```

Each run measures **9 operations** and reports both execution time (ms) and heap memory delta (KB):

| # | Operation | Method |
|---|---|---|
| 1 | Add tasks in bulk | `add()` |
| 2 | List all tasks | `findAll()` |
| 3 | Update a task | `update()` |
| 4 | Find by ID | `findById()` |
| 5 | Filter by completion | `findByCompleted()` |
| 6 | Filter due today | `findDueToday()` |
| 7 | Filter by category | `findByCategory()` |
| 8 | Filter by priority | `findByPriority()` |
| 9 | Delete a task | `delete()` |

> **Note:** Keep the terminal window open while using the GUI. Benchmark output and operation logs print to the terminal.

---

### UI Output Behaviour

| Output Location | What Appears There |
|---|---|
| Topbar (GUI) | Active data structure name, task count, load time in ns |
| All Tasks view (GUI) | Full CRUD table, search bar, filter dropdown |
| Dashboard (GUI) | Stat cards, due today list, recent tasks |
| Terminal | Per-operation benchmark lines, PerformanceTest results |

---

## Project Structure

```
src/
└── main/
    ├── java/
    │   └── com/hetero/
    │       ├── app/
    │       │   ├── HeteroApp.java          — JavaFX entry point
    │       │   ├── SessionManager.java     — holds the logged-in user
    │       │   └── ThemeManager.java       — dark / light mode switching
    │       ├── model/
    │       │   ├── Task.java               — core domain entity
    │       │   ├── Priority.java           — enum: CRITICAL to MINIMAL
    │       │   └── User.java               — immutable user record
    │       ├── repository/
    │       │   ├── TaskRepository.java     — Strategy interface (11 methods)
    │       │   ├── HashMapTaskRepo.java    — HashMap implementation
    │       │   ├── LinkedListTaskRepo.java — LinkedList implementation
    │       │   └── ArrayListTaskRepo.java  — ArrayList implementation
    │       ├── db/
    │       │   └── DatabaseManager.java    — Singleton SQLite gateway
    │       ├── controller/
    │       │   ├── MainLayoutController.java   — shell, nav, strategy swap
    │       │   ├── LoginController.java        — sign in and register
    │       │   ├── AllTasksController.java     — full CRUD table view
    │       │   ├── DashboardController.java    — stat cards and lists
    │       │   ├── TodayController.java        — tasks due today
    │       │   ├── CategoriesController.java   — tasks grouped by category
    │       │   └── SettingsController.java     — theme, account, logout
    │       └── test/
    │           └── PerformanceTest.java    — interactive benchmark tool
    └── resources/
        └── com/hetero/
            ├── css/
            │   ├── hetero-dark.css         — Notion-inspired dark theme
            │   └── hetero-light.css        — light theme counterpart
            └── fxml/
                ├── LoginView.fxml
                ├── MainLayout.fxml
                ├── dashboardView.fxml
                ├── all-tasksView.fxml
                ├── todayView.fxml
                ├── categoriesView.fxml
                └── settingsView.fxml
```

---

## Performance Analysis

### Time Complexity

| Operation | HashMap | LinkedList | ArrayList |
|---|---|---|---|
| Add task | O(1) avg | O(1) tail | O(1) amortised |
| Find by ID | **O(1) avg** | O(n) | O(n) |
| Update | **O(1) avg** | O(n) | O(n) |
| Delete | **O(1) avg** | O(n) | O(n) + shift |
| Filter / Search | O(n) | O(n) | O(n) |
| Load all | O(n) | O(n) | O(n) |

### Memory Usage

**HashMap**
- O(n) space — one entry per task
- Extra overhead from hash buckets and load factor
- Most efficient when lookups are the priority use case

**LinkedList**
- O(n) space — one node per task
- Each node carries two pointers (prev/next) adding slight overhead
- No wasted capacity — grows exactly as needed

**ArrayList**
- O(n) space in a contiguous array
- May allocate more capacity than needed during resizes
- Most cache-friendly due to contiguous memory layout

---

## Running the Application

### Prerequisites
- Java JDK 17 or higher
- Maven (or use the included `mvnw.cmd` wrapper — no install needed)
- A terminal window open alongside the app (for benchmark output)

### Build and Run

```bash
# Using the Maven wrapper (no Maven install required)
.\mvnw.cmd javafx:run

# Or if Maven is on your PATH
mvn javafx:run
```

### Run the Performance Test Tool

```bash
# The tool runs as a standalone terminal program
.\mvnw.cmd compile exec:java -Dexec.mainClass="com.hetero.test.PerformanceTest"
```

### Default Login
```
Username: admin
Password: admin
```

---

## Screens

| Screen | Description |
|---|---|
| Login | Sign in or create a new account with form validation |
| Dashboard | Four stat cards (total, pending, done, due today) plus recent task lists |
| All Tasks | Searchable, filterable table with full add / edit / delete support |
| Today | All tasks due today with inline completion checkboxes |
| Categories | Tasks automatically grouped into cards by category label |
| Settings | Dark / light theme toggle, signed-in user info, sign out |

---

## Design Decisions

**Why the Strategy Pattern?**
It lets us swap the data structure at runtime through a single interface reference. `MainLayoutController` holds one `TaskRepository` variable — it does not care whether it is a HashMap, LinkedList, or ArrayList. This is the cleanest way to make the performance comparison fair and transparent.

**Why write-through persistence?**
Every change is saved to SQLite immediately after the in-memory operation. This means the database always reflects the current state, so switching data structure modes is safe — the new collection is hydrated fresh from SQLite every time.

**Why a JVM warmup in PerformanceTest?**
The JIT compiler needs a few hundred executions of a code path before it compiles it to native machine code. Without warmup the first measurements are inflated by interpreter overhead. Running 500 operations before recording ensures the numbers reflect true collection performance.

---

## Future Improvements

- Graph visualisation of task dependencies
- Task recurrence and reminders
- Export to CSV or PDF
- Multi-user support with per-user task isolation
- Password hashing (currently stored in plaintext — not suitable for production)

---

## Contributors

Dyka Adlero Clifzier Holy Covenant (2902719013)
Ryan Richard Kalona (2902718774)
Simran Ramchandani (2902710973)

*OOP & Data Structures — [Object-Oriented Programming / Data Structures]*
*[BINUS INTERNATIONAL]*

---

## Conclusion

Hetero demonstrates the real trade-offs between three common Java data structures by applying them to the same practical problem. HashMap consistently wins on individual lookups and mutations. LinkedList avoids the memory shift cost of ArrayList on deletions. ArrayList is the most intuitive but pays a compaction penalty whenever a task is removed from the middle.

The right choice always depends on the workload:
- Use **HashMap** when you need fast access to individual tasks by id
- Use **LinkedList** when you are frequently appending and rarely looking up by id
- Use **ArrayList** when iteration order matters and deletions are rare
