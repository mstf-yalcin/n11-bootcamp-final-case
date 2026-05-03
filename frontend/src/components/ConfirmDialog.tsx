import { useState, useCallback, type ReactNode } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

type ConfirmOptions = {
  title: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
};

type ConfirmState = ConfirmOptions & {
  open: boolean;
  resolve?: (confirmed: boolean) => void;
};

const NOOP_STATE: ConfirmState = {
  open: false,
  title: "",
  confirmLabel: "Onayla",
  cancelLabel: "İptal",
};

export function useConfirm() {
  const [state, setState] = useState<ConfirmState>(NOOP_STATE);

  const confirm = useCallback((opts: ConfirmOptions): Promise<boolean> => {
    return new Promise((resolve) => {
      setState({
        ...NOOP_STATE,
        ...opts,
        open: true,
        resolve,
      });
    });
  }, []);

  const close = (result: boolean) => {
    state.resolve?.(result);
    setState(NOOP_STATE);
  };

  const dialog: ReactNode = (
    <Dialog
      open={state.open}
      onOpenChange={(open) => {
        if (!open) close(false);
      }}
    >
      <DialogContent className="sm:max-w-[420px]">
        <DialogHeader>
          <DialogTitle>{state.title}</DialogTitle>
          {state.description && (
            <DialogDescription>{state.description}</DialogDescription>
          )}
        </DialogHeader>
        <DialogFooter className="gap-2 sm:gap-2">
          <Button variant="outline" onClick={() => close(false)}>
            {state.cancelLabel ?? "İptal"}
          </Button>
          <Button
            variant={state.destructive ? "destructive" : "default"}
            onClick={() => close(true)}
          >
            {state.confirmLabel ?? "Onayla"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );

  return { confirm, dialog };
}
