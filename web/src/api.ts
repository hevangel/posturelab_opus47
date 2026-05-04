export interface FrontMetrics {
  head_shift_in: number;
  head_tilt_deg: number;
  shoulders_shift_in: number;
  shoulders_tilt_deg: number;
  ribcage_shift_in: number;
  hips_shift_in: number;
  hips_tilt_deg: number;
  q_angle_left_deg: number;
  q_angle_right_deg: number;
  total_shift_in: number;
  total_tilt_deg: number;
}

export interface SideMetrics {
  head_forward_in: number;
  head_off_vertical_deg: number;
  base_head_weight_lb: number;
  effective_head_weight_lb: number;
  shoulder_shift_in: number;
  shoulder_off_vertical_deg: number;
  hip_shift_in: number;
  hip_off_vertical_deg: number;
  knee_shift_in: number;
  head_vs_ankle_in: number;
  head_vs_ankle_flex_deg: number;
  total_shift_in: number;
  total_tilt_deg: number;
}

export interface AnalyzeResponse {
  front: FrontMetrics;
  side: SideMetrics;
  annotated_front_b64: string;
  annotated_side_b64: string;
  patient_name: string;
  report_date: string;
  patient_height_in: number;
  patient_weight_lb: number;
  base_head_weight_lb: number;
}

export interface AnalyzeInput {
  front: Blob;
  side: Blob;
  patient_name: string;
  report_date: string;
  patient_height_in: number;
  patient_weight_lb: number;
  base_head_weight_lb: number;
}

const API_BASE = (import.meta.env.VITE_API_URL as string | undefined) ?? '/api';

export async function analyze(input: AnalyzeInput): Promise<AnalyzeResponse> {
  const fd = new FormData();
  fd.append('front', input.front, 'front.jpg');
  fd.append('side', input.side, 'side.jpg');
  fd.append('patient_name', input.patient_name);
  fd.append('report_date', input.report_date);
  fd.append('patient_height_in', String(input.patient_height_in));
  fd.append('patient_weight_lb', String(input.patient_weight_lb));
  fd.append('base_head_weight_lb', String(input.base_head_weight_lb));

  const res = await fetch(`${API_BASE}/analyze`, { method: 'POST', body: fd });
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(`Analyze failed (${res.status}): ${txt}`);
  }
  return (await res.json()) as AnalyzeResponse;
}
