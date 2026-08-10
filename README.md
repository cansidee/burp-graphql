# GraphQL Schema Parser

> A Burp Suite extension (Montoya API) for GraphQL security testing.  
> Fetches the full introspection schema, classifies every operation by argument complexity, and sends ready-to-use queries straight to Repeater.

---

## Installation (no build required)

1. Download the latest JAR from [**Releases**](https://github.com/cansidee/burp-graphql/releases/latest)
2. Open Burp Suite → **Extensions** → **Installed** → **Add**
3. Extension type: **Java**
4. Select the downloaded `.jar` file → **Next**

The **GraphQL Schema Parser** tab appears at the top of the Burp Suite UI.

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

<img width="1509" height="806" alt="Screenshot 2026-05-04 at 16 43 01" src="https://github.com/user-attachments/assets/33da3522-4a3a-4157-b192-c934bd57a633" />


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

## Build from source

Requirements: Java 17+, Maven 3.8+

```bash
git clone https://github.com/cansidee/burp-graphql.git
cd burp-graphql
mvn clean package
# → target/graphql-schema-parser-1.0.0.jar
```

> Jackson is shaded into `com.burpgraphql.shaded.jackson` so it never conflicts with Burp's own classpath.

---

## Requirements

| Tool | Version |
|------|---------|
| Burp Suite | Professional or Community 2023.x+ (Montoya API) |
| Java (runtime) | 17+ |

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

Released under the [MIT License](LICENSE).
