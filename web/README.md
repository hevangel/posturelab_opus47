# PostureLab Web

React + Vite + TypeScript + Tailwind. Webcam capture or file upload, calls the AI service, renders a styled report, exports a multi-page PDF via `jsPDF` + `html2canvas`.

## Dev

```powershell
npm install
npm run dev
# open http://localhost:5173
```

The dev server proxies `/api/*` to <http://localhost:8000> (the AI service). Override with `VITE_API_URL`.

## Production build

```powershell
npm run build
npm run preview
```

Or run via Docker from the monorepo root:

```powershell
docker compose up --build
```

## Files

| File | Purpose |
| ---- | ------- |
| [src/App.tsx](src/App.tsx) | Top-level shell: form, capture, analyze, report |
| [src/PhotoSource.tsx](src/PhotoSource.tsx) | Upload + webcam component |
| [src/Report.tsx](src/Report.tsx) | Pixel-styled posture report (mirrors the reference PDF layout) |
| [src/api.ts](src/api.ts) | `POST /analyze` client |
| [src/pdf.ts](src/pdf.ts) | DOM → PDF download |
| [src/Logo.tsx](src/Logo.tsx) | Brand logo (two-arc mark) |
| [src/brand.ts](src/brand.ts) | Brand color tokens |
