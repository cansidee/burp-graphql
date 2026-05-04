# GraphQL Schema Parser

> A Burp Suite extension (Montoya API) for GraphQL security testing.  
> Fetches the full introspection schema, classifies every operation by argument complexity, and sends ready-to-use queries straight to Repeater.

---

## Features

- **Schema introspection** via standard `__schema` query — sent through Burp's HTTP engine so session-handling rules, auth headers, and cookies apply automatically
- **Tree view** — operations grouped under Queries / Mutations / Subscriptions, each split into three sub-groups:
  - `No Arguments` — fire instantly, no params needed
  - `Optional Args Only` — works without arguments, args skippable
  - `Required Args` — at least one non-null argument; full variable template generated
- **Auto-generated queries** — valid, editable GraphQL for every operation, with scalar field selections resolved one level deep
- **Send to Repeater** — single operation or the entire sub-group in one click, labelled `GraphQL: <operationName>`

---

## Screenshots

```
┌─ GraphQL Schema Parser ──────────────────────────────────────────────────┐
│ Endpoint: https://api.example.com/graphql  [Fetch Schema]  Loaded: 47 ops │
├───────────────────────────┬──────────────────────────────────────────────┤
│ GraphQL Schema            │ getUserById(id: ID!): User                   │
│ ├─ Queries                │ Return type: User                            │
│ │  ├─ No Arguments        │ Arguments:                                   │
│ │  │  └─ me               │   id   ID!   [REQUIRED]                     │
│ │  ├─ Optional Args Only  │                                              │
│ │  │  └─ users            │ Generated Query:                             │
│ │  └─ Required Args       │ query($id: ID!) {                            │
│ │     └─ getUserById  ◄   │   getUserById(id: $id) {                     │
│ ├─ Mutations              │     id                                       │
│ │  └─ Required Args       │     name                                     │
│ │     └─ createUser       │     email                                    │
│ └─ Subscriptions          │   }                                          │
│    └─ No Arguments        │ }                                            │
│       └─ onMessageAdded   │                                              │
│                           │ [Send to Repeater] [Send to Repeater (group)]│
└───────────────────────────┴──────────────────────────────────────────────┘
```

---

## Requirements

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ |
| Burp Suite | Professional or Community 2023.x+ (Montoya API) |

---

## Build

```bash
git clone https://github.com/cansidee/burp-graphql.git
cd burp-graphql
mvn clean package
```

The fat JAR (Jackson bundled, relocated to avoid classpath conflicts) is output to:

```
target/graphql-schema-parser-1.0.0.jar
```

---

## Installation

1. Open Burp Suite → **Extensions** → **Installed** → **Add**
2. Extension type: **Java**
3. Select `target/graphql-schema-parser-1.0.0.jar`
4. Click **Next**

The **GraphQL Schema Parser** tab appears at the top level of the Burp Suite UI.

---

## Usage

### 1 — Set the endpoint

```
https://api.example.com/graphql
```

### 2 — Fetch schema

Click **Fetch Schema** (or press Enter in the URL field).  
The extension POSTs the full GraphQL introspection query through Burp's HTTP stack — any active session-handling rules or cookie jars fire automatically.

### 3 — Browse the tree

Operations are classified into three buckets:

| Group | Criteria | Generated query |
|-------|----------|----------------|
| `No Arguments` | Zero args | `query { field { scalars } }` |
| `Optional Args Only` | All args nullable | `query { field { scalars } }` (args omitted) |
| `Required Args` | ≥ 1 `NON_NULL` arg | `query($x: Type!) { field(x: $x) { scalars } }` |

Click a leaf node to see the full signature, argument table, and generated query.

### 4 — Send to Repeater

| Button | Action |
|--------|--------|
| **Send to Repeater** | Sends the currently displayed (editable) query |
| **Send to Repeater (all in group)** | Sends every operation in the same sub-group |

Each Repeater tab is labelled `GraphQL: <operationName>`.  
For `Required Args` operations, the `variables` object is sent as `{}` — fill it in Repeater before firing.

---

## How it works

```
SchemaFetcher          IntrospectionParser        QueryGenerator
──────────────         ───────────────────        ──────────────
POST __schema    →     build typeMap         →    resolve return type
via Montoya HTTP        group fields by args       pick scalar leaves
                        (No Args / Opt / Req)      emit query string
                              │
                        SchemaTreePanel
                        ────────────────
                        JTree (3 levels)
                        selection → DetailPanel
                                    ──────────
                                    signature · args · query · buttons
```

**Key implementation notes:**
- `api.http().sendRequest()` — Burp proxy / session rules applied
- Jackson shaded to `com.burpgraphql.shaded.jackson` — no classpath conflict with Burp's own Jackson
- `SwingWorker` — fetch on background thread, UI update on EDT
- Siblings list shared by reference — "send all" needs zero extra traversal

---

## Project layout

```
src/main/java/com/burpgraphql/
├── extension/
│   └── GraphQLSchemaParser.java      # BurpExtension entry point
├── schema/
│   ├── model/
│   │   ├── TypeRef.java              # NON_NULL / LIST wrapper, toGraphQLString()
│   │   ├── ArgDef.java
│   │   ├── FieldDef.java             # getGroup() classification logic
│   │   ├── TypeDef.java
│   │   └── IntrospectionResponse.java
│   └── IntrospectionParser.java      # builds typeMap, splits Q/M/S
├── generator/
│   └── QueryGenerator.java           # scalar leaf selection, variable declarations
├── http/
│   └── SchemaFetcher.java            # introspection POST via Montoya Http
└── ui/
    ├── OperationContext.java          # field + opType + typeMap + siblings ref
    ├── SchemaTreePanel.java           # JTree with 3-level structure
    ├── DetailPanel.java               # signature, args, query, repeater buttons
    └── MainTab.java                   # top bar + JSplitPane, SwingWorker
```

---

## Troubleshooting

| Symptom | Likely cause |
|---------|-------------|
| `Introspection error: …` | Endpoint has introspection disabled |
| `No response from server` | URL unreachable or wrong scheme (`http` vs `https`) |
| `Failed to parse response as JSON` | Response is HTML — WAF block or login redirect |
| `Response missing __schema` | Server returned `{"data": null}` — introspection disabled silently |
| Empty tree after "Loaded: 0 ops" | Schema has no fields on the query root (unusual) |

For authenticated targets, add a Burp **Session Handling Rule** or configure the **Cookie Jar** — all requests go through `api.http().sendRequest()` so every rule applies.

---

## License

MIT
