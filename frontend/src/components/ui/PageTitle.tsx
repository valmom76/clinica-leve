import type { ReactNode } from "react";

type PageTitleProps = {
  eyebrow: string;
  title: string;
  description: string;
  action?: ReactNode;
};

export function PageTitle({
  eyebrow,
  title,
  description,
  action,
}: PageTitleProps) {
  return (
    <div className="page-title">
      <div>
        <span className="eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {action}
    </div>
  );
}
