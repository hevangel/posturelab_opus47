import { AnalyzeResponse } from './api';
import { Logo } from './Logo';

const fmt = (n: number, unit: string, digits = 2) => `${n.toFixed(digits)} ${unit}`;

function describeShift(v: number, axis: 'horizontal' | 'forward'): string {
  if (Math.abs(v) < 0.05) return axis === 'horizontal' ? 'not shifted significantly' : 'aligned';
  const dir = axis === 'horizontal' ? (v > 0 ? 'right' : 'left') : (v > 0 ? 'forward' : 'backward');
  return `shifted ${Math.abs(v).toFixed(2)} in ${dir}`;
}
function describeTilt(v: number): string {
  if (Math.abs(v) < 0.5) return 'not tilted';
  return `tilted ${Math.abs(v).toFixed(1)}° ${v > 0 ? 'right' : 'left'}`;
}

interface Props {
  data: AnalyzeResponse;
  reportRef?: React.RefObject<HTMLDivElement>;
}

export function Report({ data, reportRef }: Props) {
  const f = data.front;
  const s = data.side;

  return (
    <div ref={reportRef} className="bg-white text-slate-900 mx-auto" style={{ width: '8.5in', padding: '0.4in', fontFamily: 'Helvetica, Arial, sans-serif' }}>
      {/* Letterhead */}
      <header className="flex items-start justify-between border-b border-brand pb-3">
        <div>
          <Logo className="h-12 w-auto" />
        </div>
        <div className="text-right text-xs text-slate-600 leading-tight">
          <div className="font-semibold text-brand text-sm">PostureLab</div>
          <div>Posture Analysis Platform</div>
          <div>posturelab.example</div>
        </div>
      </header>

      {/* Intro */}
      <p className="text-[11px] leading-snug mt-3 text-slate-800">
        Good posture is simple and elegant by design in form and function. The body is designed to have the head, rib cage, and pelvis perfectly balanced upon one another in both the front and side views. If the posture is deviated from normal, then the spine is also deviated from the normal healthy position. Abnormal posture has been associated with the development and progression of many spinal conditions and injuries including: increased muscle activity and disc injury, scoliosis, work lifting injuries, sports injuries, back pain, neck pain, headaches, carpal tunnel symptoms, shoulder and ankle injuries, and many other conditions.
      </p>

      {/* Two image columns */}
      <div className="grid grid-cols-2 gap-3 mt-4">
        <Column title="Your Posture Viewed from the Front" img={data.annotated_front_b64} totalShift={f.total_shift_in} totalTilt={f.total_tilt_deg} />
        <Column title="Your Posture Viewed from the Side" img={data.annotated_side_b64} totalShift={s.total_shift_in} totalTilt={s.total_tilt_deg} />
      </div>

      {/* Findings */}
      <div className="grid grid-cols-2 gap-3 mt-3">
        {/* Front findings */}
        <div className="text-[11px] leading-snug">
          <Row tint>Head is {describeShift(f.head_shift_in, 'horizontal')}. Head is {describeTilt(f.head_tilt_deg)}.</Row>
          <Row>Shoulders are {describeShift(f.shoulders_shift_in, 'horizontal')}. Shoulders are {describeTilt(f.shoulders_tilt_deg)}.</Row>
          <Row tint>Ribcage is {describeShift(f.ribcage_shift_in, 'horizontal')}.</Row>
          <Row>Hips are {describeShift(f.hips_shift_in, 'horizontal')}. Hips are {describeTilt(f.hips_tilt_deg)}.</Row>
          <Row tint>The Right Q-Angle is {f.q_angle_right_deg.toFixed(1)}°. The Left Q-Angle is {f.q_angle_left_deg.toFixed(1)}°.</Row>
        </div>
        {/* Side findings */}
        <div className="text-[11px] leading-snug">
          <Row tint>Head is {describeShift(s.head_forward_in, 'forward')}, {s.head_off_vertical_deg.toFixed(1)}° off vertical.</Row>
          <Row>
            Based on physics, your head now weighs {s.effective_head_weight_lb.toFixed(0)} lbs instead of {s.base_head_weight_lb.toFixed(0)} lbs.
          </Row>
          <Row tint>Shoulders are {describeShift(s.shoulder_shift_in, 'forward').replace('forward', 'forward').replace('backward', 'backward')}, {s.shoulder_off_vertical_deg.toFixed(1)}° off vertical.</Row>
          <Row>Hips are {describeShift(s.hip_shift_in, 'forward')}, {s.hip_off_vertical_deg.toFixed(1)}° off vertical.</Row>
          <Row tint>Knees are {describeShift(s.knee_shift_in, 'forward')}.</Row>
          <Row>Head vs. Ankle alignment is {fmt(Math.abs(s.head_vs_ankle_in), 'in')} {s.head_vs_ankle_in >= 0 ? 'forward' : 'backward'}, {s.head_vs_ankle_flex_deg.toFixed(1)}° flexed.</Row>
        </div>
      </div>

      {/* Disclaimer */}
      <p className="text-[10.5px] mt-3 text-brand font-semibold leading-snug">
        Any measurable deviation from normal posture causes weakening of the spine as well as increased stress on the nervous system which can adversely affect overall health.
      </p>

      <footer className="text-[8.5px] text-slate-500 mt-3 border-t pt-2 leading-snug">
        Pose detection uses the open-source MediaPipe Pose Landmarker model. This report is generated automatically and is not a medical diagnosis. Report for {data.patient_name} on {data.report_date || new Date().toISOString().slice(0, 10)}.
      </footer>
    </div>
  );
}

function Column({ title, img, totalShift, totalTilt }: { title: string; img: string; totalShift: number; totalTilt: number }) {
  return (
    <div>
      <div className="band">{title}</div>
      <div className="border border-t-0 border-brand bg-white">
        <img src={`data:image/jpeg;base64,${img}`} alt={title} className="w-full h-[3.6in] object-contain bg-slate-50" />
        <div className="grid grid-cols-2 text-[10px] text-center border-t border-brand">
          <div className="py-1 border-r border-brand">
            <div className="text-brand font-semibold">Total Shift</div>
            <div>{totalShift.toFixed(2)} in</div>
          </div>
          <div className="py-1">
            <div className="text-brand font-semibold">Total Tilt</div>
            <div>{totalTilt.toFixed(1)}°</div>
          </div>
        </div>
      </div>
    </div>
  );
}

function Row({ children, tint }: { children: React.ReactNode; tint?: boolean }) {
  return <div className={`row ${tint ? 'row-tint' : ''}`}>{children}</div>;
}
