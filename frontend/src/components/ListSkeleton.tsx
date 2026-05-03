import { Skeleton } from "@/components/ui/skeleton";

export function CartItemSkeleton() {
  return (
    <div className="flex gap-4 rounded-lg border bg-white p-4">
      <Skeleton className="h-24 w-24 flex-shrink-0" />
      <div className="flex flex-1 flex-col gap-2">
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-3 w-1/3" />
        <div className="mt-auto flex items-center gap-3">
          <Skeleton className="h-8 w-24" />
          <Skeleton className="h-3 w-12" />
        </div>
      </div>
      <Skeleton className="h-5 w-20" />
    </div>
  );
}

export function OrderRowSkeleton() {
  return (
    <div className="rounded-lg border bg-white p-4">
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 space-y-2">
          <div className="flex items-center gap-2">
            <Skeleton className="h-3 w-16" />
            <Skeleton className="h-4 w-20" />
            <Skeleton className="h-5 w-24 rounded-full" />
          </div>
          <Skeleton className="h-3 w-48" />
          <div className="flex gap-1.5">
            <Skeleton className="h-5 w-20 rounded" />
            <Skeleton className="h-5 w-24 rounded" />
          </div>
        </div>
        <div className="text-right">
          <Skeleton className="h-5 w-20" />
        </div>
      </div>
    </div>
  );
}

export function AddressCardSkeleton() {
  return (
    <div className="rounded-lg border bg-white p-4">
      <Skeleton className="mb-2 h-4 w-24" />
      <Skeleton className="mb-2 h-3 w-32" />
      <Skeleton className="h-3 w-full" />
      <Skeleton className="mt-1 h-3 w-3/4" />
    </div>
  );
}

export function PaymentRowSkeleton() {
  return (
    <div className="flex items-center justify-between gap-4 rounded-lg border bg-white p-4">
      <div className="flex-1 space-y-1.5">
        <div className="flex items-center gap-2">
          <Skeleton className="h-3 w-16" />
          <Skeleton className="h-4 w-20" />
          <Skeleton className="h-5 w-20 rounded-full" />
        </div>
        <Skeleton className="h-3 w-32" />
      </div>
      <Skeleton className="h-5 w-20" />
      <Skeleton className="h-8 w-8" />
    </div>
  );
}
