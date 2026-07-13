import type { CSSProperties } from "react";

type ChartPoint = {
  key: string;
  label: string;
  first: number;
  second: number;
};

type GroupedBarChartProps = {
  points: ChartPoint[];
  firstLabel: string;
  secondLabel: string;
  firstClass: string;
  secondClass: string;
  formatValue: (value: number) => string;
  ariaLabel: string;
};

export function GroupedBarChart({
  points,
  firstLabel,
  secondLabel,
  firstClass,
  secondClass,
  formatValue,
  ariaLabel,
}: GroupedBarChartProps) {
  const maximum = Math.max(1, ...points.flatMap((point) => [point.first, point.second]));
  return (
    <figure className="report-chart">
      <figcaption className="report-chart-legend">
        <span className={firstClass}>{firstLabel}</span>
        <span className={secondClass}>{secondLabel}</span>
      </figcaption>
      <div className="report-chart-scroll">
        <div className="report-chart-bars" role="img" aria-label={ariaLabel}>
          {points.map((point) => (
            <div className="report-bar-column" key={point.key}>
              <div className="report-bar-pair">
                <i
                  className={firstClass}
                  style={{ "--bar-height": `${Math.max(point.first > 0 ? 4 : 1, point.first / maximum * 100)}%` } as CSSProperties}
                  title={`${point.label}: ${firstLabel} ${formatValue(point.first)}`}
                />
                <i
                  className={secondClass}
                  style={{ "--bar-height": `${Math.max(point.second > 0 ? 4 : 1, point.second / maximum * 100)}%` } as CSSProperties}
                  title={`${point.label}: ${secondLabel} ${formatValue(point.second)}`}
                />
              </div>
              <small>{point.label}</small>
            </div>
          ))}
        </div>
      </div>
    </figure>
  );
}
