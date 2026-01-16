# Quarkus Dashboard Generator

Generate SVG and PNG dashboard visualizations from hierarchical JSON payloads, or build ESA payloads directly from Jira.

## Features

- **SVG Rendering**: POST JSON → receive `image/svg+xml`
- **PNG Conversion**: POST JSON → SVG → PNG via Apache Batik (`image/png`)
- **HTML Preview**: POST JSON → receive HTML page with inline SVG and PNG for easy visual review
- **OpenAPI/Swagger UI**: Interactive API docs with pre-filled examples at `/q/swagger-ui`
- **Jira Importer**: Use `/ui/jira` to discover ESA root issues and generate ESA payloads directly from Jira. Provide the HTTPS Jira base URL, a personal access token (PAT) with read rights, and any extra Jira headers (one per line) to be forwarded on the Jira API call; credentials are supplied per request so nothing is stored server-side.
- **Jira ESA API**: POST `/api/v1/jira/esa` with a Jira issue URL (and optional base URL), optional headers, and attribute pairs to generate ESA JSON.
- **Custom Trust Store**: If your Jira instance uses a private CA, point the app at a trust store via `jira.trust-store` / `jira.trust-store-password` in `application.properties` and all outbound HTTPS calls will honor it.

## Quick Start

1. Start dev mode:
```powershell
mvn quarkus:dev
```

2. Open Swagger UI:
```
http://localhost:8080/q/swagger-ui
```

3. Try the endpoints with the built-in sample payload

## API Endpoints

### POST `/api/v1/dashboard/svg`
Renders an SVG dashboard. Returns `image/svg+xml`.

### POST `/api/v1/dashboard/png`
Renders a PNG dashboard (SVG → PNG via Batik). Returns `image/png`.

### POST `/api/v1/dashboard/preview`
Renders an HTML page with inline SVG and PNG for easy visual review in the browser. Returns `text/html`.

### POST `/api/v1/jira/esa`
Generates ESA JSON from a Jira root issue URL. Accepts optional attribute pairs that are included in the JSON response.

## Jira Discovery and Import

Use the UI to discover ESA roots and generate ESA payloads:

1. Open `http://localhost:8080/ui/jira`.
2. Enter Jira location, PAT, and any extra headers.
3. Click **Discover ESA Roots** to list issues labeled `ESA-Root`.
4. Select a root issue and click **Fetch & Generate JSON**.

Notes:
- The root issue must include labels `ESA` and `ESA-Root:{name}`.
- The importer follows linked issues for governance and capabilities per the ESA rules described in the UI.
- Extra headers are sent on every Jira API call; `Authorization` and `Accept` are always controlled by the app.

## Jira ESA API Example

```json
{
  "jiraUrl": "https://jira.example.com/browse/ESA-123",
  "jiraBase": "https://jira.example.com",
  "jiraToken": "<personal-access-token>",
  "headers": [
    { "name": "X-Atlassian-Token", "value": "no-check" }
  ],
  "attributes": [
    { "name": "owner", "value": "AppSec" },
    { "name": "portfolio", "value": "Security" }
  ]
}
```

## JSON Structure

The endpoints accept a hierarchical JSON payload:

```json
{
  "title": "Application Security",
  "icon": "shield",
  "governance": {
    "title": "Application Security Governance",
    "components": [
      {
        "name": "Static Code Scanning",
        "maturity": "MANAGED",
        "status": "HIGH",
        "initiatives": 2,
        "iRag": "green",
        "doubleBorder": true,
        "initiative": [
          {
            "key": "AS-001",
            "link": "https://example.com/initiatives/AS-001",
            "summary": "Expand SAST rules coverage",
            "businessBenefit": "Detect critical issues earlier",
            "riskAppetite": "Low",
            "toolId": "In-Demand",
            "dueDate": "2024-12-15",
            "rag": "green"
          },
          {
            "key": "AS-001b",
            "link": "https://example.com/initiatives/AS-001b",
            "summary": "Introduce SAST guardrails in portals",
            "businessBenefit": "Improve self-service onboarding",
            "riskAppetite": "Low",
            "toolId": "In-Demand",
            "dueDate": "2025-04-30",
            "rag": "green"
          }
        ]
      },
      {
        "name": "RASP Agent",
        "maturity": "DEFINED",
        "status": "MEDIUM",
        "initiatives": 1,
        "iRag": "amber",
        "initiative": [
          {
            "key": "AS-002",
            "link": "https://example.com/initiatives/AS-002",
            "summary": "Pilot runtime protection",
            "businessBenefit": "Reduce exploitation risk",
            "riskAppetite": "Medium",
            "toolId": "In-Demand",
            "dueDate": "2025-02-01",
            "rag": "amber"
          }
        ]
      }
    ]
  },
  "capabilities": {
    "title": "Application Security Capabilities",
    "domains": [
      {
        "domain":"Application Security Testing",
        "components": [
          {
            "name": "Static Code Scanning",
            "maturity": "MANAGED",
            "status": "HIGH",
            "initiatives": 3,
            "initiative": [
              {
                "key": "AS-003",
                "link": "https://example.com/initiatives/AS-003",
                "summary": "Automate onboarding",
                "businessBenefit": "Increase coverage quickly",
                "riskAppetite": "Low",
                "toolId": "In-Demand",
                "dueDate": "2025-01-20",
                "rag": "green"
              }
            ]
          },
          {
            "name": "RASP Agent",
            "maturity": "DEFINED",
            "status": "MEDIUM",
            "initiatives": 1,
            "initiative": [
              {
                "key": "AS-004",
                "link": "https://example.com/initiatives/AS-004",
                "summary": "Expand telemetry",
                "businessBenefit": "Improve runtime visibility",
                "riskAppetite": "Medium",
                "toolId": "In-Demand",
                "dueDate": "2024-11-30",
                "rag": "amber"
              }
            ]
          }
        ]
      },
      {
        "domain":"RASP Security Testing",
        "components": [
          {
            "name": "Runtime Chaos",
            "maturity": "REPEATABLE",
            "status": "LOW",
            "initiatives": 2,
            "initiative": [
              {
                "key": "AS-005",
                "link": "https://example.com/initiatives/AS-005",
                "summary": "Add fuzzing scenarios",
                "businessBenefit": "Catch runtime issues earlier",
                "riskAppetite": "Medium",
                "toolId": "In-Demand",
                "dueDate": "2024-10-10",
                "rag": "amber"
              }
            ]
          },
          {
            "name": "RASP Agent Enhancements",
            "maturity": "INITIAL",
            "status": "MEDIUM",
            "initiatives": 2,
            "initiative": [
              {
                "key": "AS-006",
                "link": "https://example.com/initiatives/AS-006",
                "summary": "Harden agent policies",
                "businessBenefit": "Mitigate runtime threats",
                "riskAppetite": "High",
                "toolId": "In-Demand",
                "dueDate": "2024-09-01",
                "rag": "red"
              },
              {
                "key": "AS-006b",
                "link": "https://example.com/initiatives/AS-006b",
                "summary": "Add runtime chaos drills",
                "businessBenefit": "Validate policies against attacks",
                "riskAppetite": "High",
                "toolId": "In-Demand",
                "dueDate": "2024-11-05",
                "rag": "red"
              }
            ]
          }
        ]
      }
    ]
  }
}
```

## PowerShell Test Scripts

Run both SVG and PNG tests with the sample payload:

```powershell
pwsh -File .\scripts\test-all.ps1
```

Outputs will be written to `out/`:
- `out/dashboard.svg`
- `out/dashboard.png`

Run individually:
```powershell
pwsh -File .\scripts\test-svg.ps1
pwsh -File .\scripts\test-png.ps1
```

Override defaults:
```powershell
pwsh -File .\scripts\test-svg.ps1 -HostUrl http://localhost:8081 -Payload .\sample\custom.json -OutDir .\output
```

## Visual Preview

For quick visual feedback, use the preview endpoint in Swagger UI or via curl:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/dashboard/preview" `
  -ContentType "application/json" `
  -InFile "sample\test-payload.json" `
  -OutFile "preview.html"

Start-Process "preview.html"
```

This generates an HTML page with both SVG (inline) and PNG (base64 data URI) for side-by-side comparison.

## Testing

Run unit tests:
```powershell
mvn test
```

Tests validate:
- Endpoint responses (200, correct content types)
- Full-width blue title bars in SVG
- Governance and capabilities sections present
- Domain column headers and component boxes render correctly

## Layout

- **Main Title Bar**: Full-width blue bar at top
- **Governance Section**: Horizontal row of governance components with blue title bar
- **Capabilities Section**: Vertical columns per domain with blue title bar and domain headers
  - Each domain's components stack vertically within its column
  - Color-coded maturity: high=green, medium=orange, low=red
  - Red border indicates "new" status

## License

Licensed under the Apache License, Version 2.0. See `LICENSE`.
