import { Fragment, type ReactNode } from "react";
import { ArrowDown, ArrowUp, ArrowUpDown } from "lucide-react";
import { cn } from "@/lib/utils";

export type SortState = { key: string; direction: "asc" | "desc" };

export type Column<T> = {
  key: string;
  header: ReactNode;
  cell: (row: T) => ReactNode;
  className?: string;
  width?: string;
  sortKey?: string;
};

type Props<T> = {
  columns: Column<T>[];
  rows: T[] | undefined;
  rowKey: (row: T) => string;
  isLoading?: boolean;
  isError?: boolean;
  errorMessage?: ReactNode;
  emptyMessage?: ReactNode;
  skeletonRows?: number;
  onRowClick?: (row: T) => void;
  expandedRowKey?: string | null;
  renderExpandedRow?: (row: T) => ReactNode;
  sort?: SortState | null;
  onSort?: (sortKey: string) => void;
};

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  isLoading,
  isError,
  errorMessage,
  emptyMessage,
  skeletonRows = 5,
  onRowClick,
  expandedRowKey,
  renderExpandedRow,
  sort,
  onSort,
}: Props<T>) {
  return (
    <div className="overflow-hidden rounded-lg border bg-white">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="border-b bg-secondary/40 text-left">
            <tr>
              {columns.map((col) => {
                const sortable = Boolean(col.sortKey && onSort);
                const active = sortable && sort?.key === col.sortKey;
                return (
                  <th
                    key={col.key}
                    className={cn(
                      "whitespace-nowrap px-4 py-3 text-xs font-semibold uppercase text-muted-foreground",
                      sortable && "cursor-pointer select-none transition-colors hover:text-foreground",
                      col.className
                    )}
                    style={col.width ? { width: col.width } : undefined}
                    onClick={sortable ? () => onSort!(col.sortKey!) : undefined}
                    aria-sort={
                      active
                        ? sort!.direction === "asc"
                          ? "ascending"
                          : "descending"
                        : sortable
                          ? "none"
                          : undefined
                    }
                  >
                    <span className="inline-flex items-center gap-1">
                      {col.header}
                      {sortable &&
                        (active ? (
                          sort!.direction === "asc" ? (
                            <ArrowUp className="h-3 w-3" />
                          ) : (
                            <ArrowDown className="h-3 w-3" />
                          )
                        ) : (
                          <ArrowUpDown className="h-3 w-3 opacity-40" />
                        ))}
                    </span>
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody>
            {isLoading &&
              Array.from({ length: skeletonRows }).map((_, i) => (
                <tr key={`s-${i}`} className="border-b last:border-b-0">
                  {columns.map((col) => (
                    <td key={col.key} className="px-4 py-3">
                      <div className="h-4 w-full max-w-[120px] animate-pulse rounded bg-secondary" />
                    </td>
                  ))}
                </tr>
              ))}

            {!isLoading && isError && (
              <tr>
                <td
                  colSpan={columns.length}
                  className="px-4 py-10 text-center text-sm text-destructive"
                >
                  {errorMessage ?? "Veriler yüklenemedi."}
                </td>
              </tr>
            )}

            {!isLoading && !isError && rows && rows.length === 0 && (
              <tr>
                <td
                  colSpan={columns.length}
                  className="px-4 py-10 text-center text-sm text-muted-foreground"
                >
                  {emptyMessage ?? "Henüz kayıt yok."}
                </td>
              </tr>
            )}

            {!isLoading &&
              !isError &&
              rows?.map((row) => {
                const key = rowKey(row);
                const isExpanded =
                  renderExpandedRow != null && expandedRowKey === key;
                return (
                  <Fragment key={key}>
                    <tr
                      onClick={onRowClick ? () => onRowClick(row) : undefined}
                      className={cn(
                        "border-b last:border-b-0",
                        onRowClick && "cursor-pointer hover:bg-accent/40",
                        isExpanded && "bg-accent/30"
                      )}
                    >
                      {columns.map((col) => (
                        <td
                          key={col.key}
                          className={cn(
                            "px-4 py-3 align-top",
                            col.className
                          )}
                        >
                          {col.cell(row)}
                        </td>
                      ))}
                    </tr>
                    {isExpanded && (
                      <tr className="border-b bg-secondary/20 last:border-b-0">
                        <td
                          colSpan={columns.length}
                          className="px-4 py-4"
                        >
                          {renderExpandedRow(row)}
                        </td>
                      </tr>
                    )}
                  </Fragment>
                );
              })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
