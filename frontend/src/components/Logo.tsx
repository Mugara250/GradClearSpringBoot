/**
 * GradClear mark: a mortarboard whose tassel ends in a check badge
 * ("graduation" + "cleared"). Flat shapes in two brand blues plus white,
 * so it still reads at 24px.
 */
export function Logo({ size = 32, className }: { size?: number; className?: string }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 40 40"
      role="img"
      aria-label="GradClear"
      className={className}
    >
      {/* cap body */}
      <path d="M9 17v7.2c0 2.6 5 4.8 11 4.8s11-2.2 11-4.8V17l-11 4.6z" fill="var(--color-brand-600)" />
      {/* board */}
      <path d="M20 5 38 12.5 20 20 2 12.5z" fill="var(--color-brand-900)" />
      {/* tassel cord */}
      <path d="M20 12.5 33 15.5v8" fill="none" stroke="var(--color-brand-900)" strokeWidth="1.8" strokeLinecap="round" />
      {/* check badge where the tassel hangs */}
      <circle cx="33" cy="30" r="7" fill="var(--color-brand-400)" stroke="#fff" strokeWidth="1.5" />
      <path d="m29.6 30.2 2.4 2.4 4.6-4.8" fill="none" stroke="#fff" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
