import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';

export async function downloadReportPdf(node: HTMLElement, filename: string) {
  // Render the DOM at 2x scale for crispness.
  const canvas = await html2canvas(node, { scale: 2, backgroundColor: '#ffffff', useCORS: true });
  const imgData = canvas.toDataURL('image/jpeg', 0.92);
  const pdf = new jsPDF({ unit: 'in', format: 'letter', orientation: 'portrait' });
  const pageW = pdf.internal.pageSize.getWidth();
  const pageH = pdf.internal.pageSize.getHeight();
  // Fit image to page
  const imgRatio = canvas.height / canvas.width;
  let w = pageW;
  let h = w * imgRatio;
  if (h > pageH) {
    h = pageH;
    w = h / imgRatio;
  }
  const x = (pageW - w) / 2;
  const y = (pageH - h) / 2;
  pdf.addImage(imgData, 'JPEG', x, y, w, h);
  pdf.save(filename);
}
