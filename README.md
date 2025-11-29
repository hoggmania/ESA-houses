# Quarkus Dashboard Generator

Generate SVG and PNG dashboard visualizations from hierarchical JSON payloads.

## Features

- **SVG Rendering**: POST JSON → receive `image/svg+xml`
- **PNG Conversion**: POST JSON → SVG → PNG via Apache Batik (`image/png`)
- **HTML Preview**: POST JSON → receive HTML page with inline SVG and PNG for easy visual review
- **OpenAPI/Swagger UI**: Interactive API docs with pre-filled examples at `/q/swagger-ui`

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

## JSON Structure

The endpoints accept a hierarchical JSON payload:

```json
{
  "title": "Application Security",
  "governance": {
    "title": "Application Security Governance",
    "components ": [
      {"capability":"SAST","name":"Static Code Scanning","maturity":"high","status":"increase"},
      {"capability":"RASP","name":"RASP Agent","maturity":"medium","status":"new"}
    ]
  },
  "capabilities": {
    "title": "Application Security Capabilities",
    "domains": [
      {
        "domain":"Application Security Testing",
        "components ": [
          {"capability":"SAST","name":"Static Code Scanning","maturity":"high","status":"increase"},
          {"capability":"RASP","name":"RASP Agent","maturity":"medium","status":"new"}
        ]
      },
      {
        "domain":"RASP Security Testing",
        "components ": [
          {"capability":"SAST","name":"Static Code Scanning","maturity":"high","status":"increase"},
          {"capability":"RASP","name":"RASP Agent","maturity":"medium","status":"new"}
        ]
      }
    ]
  }
}
```

**Note**: The key `"components "` (with trailing space) is intentionally supported for backward compatibility.

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
