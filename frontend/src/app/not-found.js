import Link from "next/link";

export default function NotFound() {
  return (
    <div className="page-shell state-shell-v2">
      <div className="container-shell">
        <section className="panel state-card-v2">
          <p className="section-label">404</p>
          <h1 className="state-title-v2">Page not found</h1>
          <p className="state-copy-v2">
            The route you requested does not exist or may have been moved.
          </p>
          <Link href="/" className="button button-primary state-button-v2">
            Go Home
          </Link>
        </section>
      </div>
    </div>
  );
}
