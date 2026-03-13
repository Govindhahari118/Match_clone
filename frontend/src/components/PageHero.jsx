import Link from "next/link";

export default function PageHero({
  eyebrow,
  title,
  copy,
  note,
  actions,
  className = "",
  actionAlign = "end",
  children,
}) {
  const hasActions = Boolean(actions);
  const actionClass =
    actionAlign === "start" ? "page-hero-actions page-hero-actions-start" : "page-hero-actions";

  return (
    <section className={`page-hero ${className}`.trim()}>
      <div>
        {eyebrow && <p className="section-label">{eyebrow}</p>}
        {title && <h1 className="page-hero-title">{title}</h1>}
        {copy && <p className="page-hero-copy">{copy}</p>}
        {note && <p className="page-hero-note">{note}</p>}
        {children}
      </div>
      {hasActions && <div className={actionClass}>{actions}</div>}
    </section>
  );
}

export function PageHeroLink({ href, label, variant = "secondary" }) {
  const className = variant === "primary" ? "button button-primary" : "button button-secondary";
  return (
    <Link href={href} className={className}>
      {label}
    </Link>
  );
}
