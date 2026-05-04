import { BRAND } from './brand';

export const Logo = ({ className = '' }: { className?: string }) => (
  <svg viewBox="0 0 200 80" xmlns="http://www.w3.org/2000/svg" className={className} role="img" aria-label="PostureLab">
    <g fill="none" strokeWidth={6} strokeLinecap="round">
      <path d="M14 50 Q14 22 38 22 Q60 22 60 44 Q60 58 46 58" stroke={BRAND.primaryLight} />
      <path d="M30 30 Q54 30 54 52 Q54 70 36 70 Q18 70 18 54" stroke={BRAND.primary} />
    </g>
    <text x="74" y="38" fontFamily="Helvetica, Arial, sans-serif" fontWeight={700} fontSize={22} fill={BRAND.primary} letterSpacing={0.5}>Posture</text>
    <text x="74" y="62" fontFamily="Helvetica, Arial, sans-serif" fontWeight={700} fontSize={22} fill={BRAND.primaryLight} letterSpacing={0.5}>Lab</text>
  </svg>
);

export const LogoMark = ({ size = 28 }: { size?: number }) => (
  <svg width={size} height={size * 0.4} viewBox="0 0 200 80">
    <g fill="none" strokeWidth={6} strokeLinecap="round">
      <path d="M14 50 Q14 22 38 22 Q60 22 60 44 Q60 58 46 58" stroke={BRAND.primaryLight} />
      <path d="M30 30 Q54 30 54 52 Q54 70 36 70 Q18 70 18 54" stroke={BRAND.primary} />
    </g>
  </svg>
);
