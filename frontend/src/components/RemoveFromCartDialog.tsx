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

export type RemoveOutcome = "cancel" | "remove" | "favorite-and-remove";

type Options = {
  productName: string;
  alreadyInFavorites?: boolean;
};

type State = Options & {
  open: boolean;
  resolve?: (outcome: RemoveOutcome) => void;
};

const NOOP_STATE: State = {
  open: false,
  productName: "",
  alreadyInFavorites: false,
};

export function useRemoveFromCartDialog() {
  const [state, setState] = useState<State>(NOOP_STATE);

  const ask = useCallback((opts: Options): Promise<RemoveOutcome> => {
    return new Promise((resolve) => {
      setState({ ...NOOP_STATE, ...opts, open: true, resolve });
    });
  }, []);

  const close = (outcome: RemoveOutcome) => {
    state.resolve?.(outcome);
    setState(NOOP_STATE);
  };

  const dialog: ReactNode = (
    <Dialog
      open={state.open}
      onOpenChange={(open) => {
        if (!open) close("cancel");
      }}
    >
      <DialogContent className="sm:max-w-[460px]">
        <DialogHeader>
          <DialogTitle>Ürünü sepetten çıkar</DialogTitle>
          <DialogDescription>
            "{state.productName}" sepetinden çıkarılacak. Sonra almak istersen
            favorilere ekleyebilirsin.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="gap-2 sm:gap-2">
          <Button
            variant="outline"
            onClick={() => close("remove")}
          >
            Sil
          </Button>
          <Button
            onClick={() => close("favorite-and-remove")}
            disabled={state.alreadyInFavorites}
            className="bg-neutral-900 text-white shadow-sm hover:bg-neutral-800"
          >
            {state.alreadyInFavorites
              ? "Zaten favorilerde — Sil"
              : "Sil ve Favorilere Ekle"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );

  return { ask, dialog };
}
