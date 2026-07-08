import { useEffect } from "react";
import { useBlocker } from "react-router-dom";

// Hook cảnh báo thay đổi chưa lưu
export function useUnsavedChanges(isDirty: boolean) {
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      isDirty && currentLocation.pathname !== nextLocation.pathname
  );

  useEffect(() => {
    if (blocker.state === "blocked" && !isDirty) {
      blocker.reset?.();
    }
  }, [blocker, isDirty]);

  useEffect(() => {
    if (!isDirty) return;

    // Xử lý before unload
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault();
      // Cảnh báo khi reload/đóng tab
    };
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [isDirty]);

  return blocker;
}
