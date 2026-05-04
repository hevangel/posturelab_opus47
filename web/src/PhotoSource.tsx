import { useEffect, useRef, useState } from 'react';

interface Props {
  label: string;
  onCapture: (blob: Blob) => void;
}

export function PhotoSource({ label, onCapture }: Props) {
  const [preview, setPreview] = useState<string | null>(null);
  const [streaming, setStreaming] = useState(false);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);

  useEffect(() => () => stopStream(), []);

  function stopStream() {
    streamRef.current?.getTracks().forEach((t) => t.stop());
    streamRef.current = null;
    setStreaming(false);
  }

  async function startWebcam() {
    try {
      const s = await navigator.mediaDevices.getUserMedia({ video: { width: 1080, height: 1440 }, audio: false });
      streamRef.current = s;
      if (videoRef.current) {
        videoRef.current.srcObject = s;
        await videoRef.current.play();
      }
      setStreaming(true);
    } catch (e) {
      alert('Could not open webcam: ' + (e as Error).message);
    }
  }

  async function snap() {
    const v = videoRef.current;
    if (!v) return;
    const c = document.createElement('canvas');
    c.width = v.videoWidth;
    c.height = v.videoHeight;
    c.getContext('2d')!.drawImage(v, 0, 0);
    const blob: Blob = await new Promise((res) => c.toBlob((b) => res(b!), 'image/jpeg', 0.92)!);
    setPreview(URL.createObjectURL(blob));
    onCapture(blob);
    stopStream();
  }

  function onFile(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0];
    if (!f) return;
    setPreview(URL.createObjectURL(f));
    onCapture(f);
  }

  return (
    <div className="border rounded-lg p-3 bg-white shadow-sm">
      <div className="font-semibold text-brand mb-2">{label}</div>
      {preview && !streaming ? (
        <img src={preview} alt={label} className="w-full max-h-80 object-contain rounded bg-slate-100" />
      ) : streaming ? (
        <video ref={videoRef} className="w-full max-h-80 object-contain rounded bg-black" muted playsInline />
      ) : (
        <div className="aspect-[3/4] flex items-center justify-center text-slate-400 bg-slate-100 rounded">No image</div>
      )}
      <div className="flex flex-wrap gap-2 mt-3">
        <label className="btn-secondary cursor-pointer">
          Upload
          <input type="file" accept="image/*" className="hidden" onChange={onFile} />
        </label>
        {!streaming ? (
          <button className="btn-secondary" onClick={startWebcam} type="button">Use webcam</button>
        ) : (
          <>
            <button className="btn-primary" onClick={snap} type="button">Capture</button>
            <button className="btn-secondary" onClick={stopStream} type="button">Cancel</button>
          </>
        )}
      </div>
    </div>
  );
}
