"use client";

import { useEffect } from "react";
import { toast } from "react-toastify";
import api from "@/services/api";
import { flushOfflineQueue, getOfflineQueueLength } from "@/services/offlineQueue";

async function syncOfflineQueue() {
  const result = await flushOfflineQueue((requestConfig) =>
    api.request({
      ...requestConfig,
      headers: {
        ...(requestConfig.headers || {}),
        "x-offline-replay": "1",
      },
    }),
  );
  return result;
}

export default function RuntimeClientBootstrap() {
  useEffect(() => {
    if (typeof window === "undefined" || !("serviceWorker" in navigator)) {
      return undefined;
    }

    if (process.env.NODE_ENV !== "production") {
      const cleanupDevServiceWorkers = async () => {
        try {
          const registrations = await navigator.serviceWorker.getRegistrations();
          await Promise.allSettled(registrations.map((registration) => registration.unregister()));
        } catch {
          // Ignore cleanup failures in development.
        }

        if (typeof window.caches !== "undefined") {
          try {
            const keys = await window.caches.keys();
            await Promise.allSettled(
              keys
                .filter((key) => key.startsWith("match-static-") || key.startsWith("match-runtime-"))
                .map((key) => window.caches.delete(key))
            );
          } catch {
            // Ignore cache cleanup failures in development.
          }
        }
      };

      void cleanupDevServiceWorkers();
      return undefined;
    }

    const register = async () => {
      try {
        const registration = await navigator.serviceWorker.register("/sw.js");
        registration.update().catch(() => undefined);
      } catch {
        // Ignore SW registration failures in unsupported/private environments.
      }
    };
    void register();
    return undefined;
  }, []);

  useEffect(() => {
    if (typeof window === "undefined") {
      return undefined;
    }

    const handleOnline = async () => {
      const pending = getOfflineQueueLength();
      if (!pending) return;
      const { flushed, failed } = await syncOfflineQueue();
      if (flushed > 0) {
        toast.success(`${flushed} queued action${flushed > 1 ? "s" : ""} synced.`);
      }
      if (failed > 0) {
        toast.warn(`${failed} queued action${failed > 1 ? "s" : ""} still pending.`);
      }
    };

    window.addEventListener("online", handleOnline);
    void handleOnline();

    return () => {
      window.removeEventListener("online", handleOnline);
    };
  }, []);

  return null;
}
