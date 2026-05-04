import { useRef, useState } from 'react';
import { Logo } from './Logo';
import { PhotoSource } from './PhotoSource';
import { analyze, AnalyzeResponse } from './api';
import { Report } from './Report';
import { downloadReportPdf } from './pdf';

export function App() {
  const [front, setFront] = useState<Blob | null>(null);
  const [side, setSide] = useState<Blob | null>(null);
  const [name, setName] = useState('Patient');
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [heightIn, setHeightIn] = useState(68);
  const [weightLb, setWeightLb] = useState(160);
  const [headLb, setHeadLb] = useState(11);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<AnalyzeResponse | null>(null);
  const reportRef = useRef<HTMLDivElement>(null);

  async function onAnalyze() {
    if (!front || !side) {
      setError('Please provide both a front and a side photo.');
      return;
    }
    setError(null);
    setBusy(true);
    try {
      const r = await analyze({
        front, side,
        patient_name: name,
        report_date: date,
        patient_height_in: heightIn,
        patient_weight_lb: weightLb,
        base_head_weight_lb: headLb,
      });
      setResult(r);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function onDownload() {
    if (reportRef.current) {
      await downloadReportPdf(reportRef.current, `posturelab-${name.replace(/\s+/g, '_')}-${date}.pdf`);
    }
  }

  return (
    <div className="min-h-full">
      <header className="bg-brand text-white">
        <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="bg-white rounded p-1">
              <Logo className="h-8 w-auto" />
            </div>
            <div>
              <div className="font-bold text-lg leading-tight">PostureLab</div>
              <div className="text-xs opacity-90">Posture Analysis Platform</div>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto p-4 space-y-4">
        <section className="bg-white rounded-lg shadow-sm p-4">
          <h2 className="text-brand font-semibold mb-3">Patient Info</h2>
          <div className="grid grid-cols-2 md:grid-cols-5 gap-3 text-sm">
            <Field label="Name"><input className="input" value={name} onChange={(e) => setName(e.target.value)} /></Field>
            <Field label="Date"><input type="date" className="input" value={date} onChange={(e) => setDate(e.target.value)} /></Field>
            <Field label="Height (in)"><input type="number" className="input" value={heightIn} onChange={(e) => setHeightIn(+e.target.value)} /></Field>
            <Field label="Weight (lb)"><input type="number" className="input" value={weightLb} onChange={(e) => setWeightLb(+e.target.value)} /></Field>
            <Field label="Head wt (lb)"><input type="number" className="input" value={headLb} onChange={(e) => setHeadLb(+e.target.value)} /></Field>
          </div>
        </section>

        <section className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <PhotoSource label="Front view photo" onCapture={setFront} />
          <PhotoSource label="Side view photo" onCapture={setSide} />
        </section>

        {error && <div className="bg-red-50 border border-red-300 text-red-800 rounded p-3 text-sm">{error}</div>}

        <div className="flex gap-3">
          <button className="btn-primary" onClick={onAnalyze} disabled={busy || !front || !side}>
            {busy ? 'Analyzing…' : 'Analyze posture'}
          </button>
          {result && <button className="btn-secondary" onClick={onDownload}>Download PDF</button>}
        </div>

        {result && (
          <section className="mt-4 overflow-x-auto">
            <div className="inline-block shadow-lg">
              <Report data={result} reportRef={reportRef} />
            </div>
          </section>
        )}
      </main>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-xs text-slate-600">{label}</span>
      {children}
    </label>
  );
}
