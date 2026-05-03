import { Component, type ReactNode } from "react";
import { AlertTriangle, RefreshCcw } from "lucide-react";
import { Button } from "@/components/ui/button";

type Props = { children: ReactNode };
type State = { error: Error | null };

export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    console.error("UI crash:", error, info);
  }

  render() {
    if (!this.state.error) return this.props.children;
    return (
      <div className="flex min-h-screen items-center justify-center bg-background p-6">
        <div className="w-full max-w-md rounded-lg border bg-white p-8 text-center shadow-sm">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-destructive/10">
            <AlertTriangle className="h-7 w-7 text-destructive" />
          </div>
          <h1 className="mb-2 text-xl font-semibold">Bir şeyler ters gitti</h1>
          <p className="mb-6 text-sm text-muted-foreground">
            Beklenmeyen bir hata oluştu. Sayfayı yeniden yükleyebilir veya
            anasayfaya dönebilirsin.
          </p>
          <details className="mb-4 rounded border bg-secondary/50 p-3 text-left text-xs">
            <summary className="cursor-pointer font-medium">
              Teknik detay
            </summary>
            <pre className="mt-2 overflow-auto whitespace-pre-wrap break-words text-[11px] text-muted-foreground">
              {this.state.error.message}
            </pre>
          </details>
          <div className="flex justify-center gap-2">
            <Button
              variant="outline"
              onClick={() => {
                this.setState({ error: null });
                window.location.href = "/";
              }}
            >
              Anasayfa
            </Button>
            <Button onClick={() => window.location.reload()}>
              <RefreshCcw className="mr-2 h-4 w-4" />
              Yeniden Yükle
            </Button>
          </div>
        </div>
      </div>
    );
  }
}
