"use client";

import { createContext, useCallback, useContext, useMemo, useSyncExternalStore } from "react";

const ThemeContext = createContext({
  theme: "light",
  setTheme: () => {},
  toggleTheme: () => {},
});

const themeListeners = new Set();
let themeSnapshot = "light";
let themeInitialized = false;

function notifyThemeChange() {
  themeListeners.forEach((listener) => listener());
}

function applyTheme(theme) {
  if (typeof document === "undefined") return;
  document.documentElement.dataset.theme = theme;
}

function readClientTheme() {
  try {
    const stored = window.localStorage.getItem("theme");
    if (stored === "light" || stored === "dark") return stored;

    const prefersDark = window.matchMedia?.("(prefers-color-scheme: dark)")?.matches;
    return prefersDark ? "dark" : "light";
  } catch {
    return "light";
  }
}

function ensureThemeInitialized() {
  if (typeof window === "undefined" || themeInitialized) return;
  themeSnapshot = readClientTheme();
  applyTheme(themeSnapshot);
  themeInitialized = true;
}

function subscribeTheme(listener) {
  themeListeners.add(listener);
  return () => {
    themeListeners.delete(listener);
  };
}

function getThemeSnapshot() {
  ensureThemeInitialized();
  return themeSnapshot;
}

function getThemeServerSnapshot() {
  return "light";
}

function commitTheme(nextTheme) {
  themeSnapshot = nextTheme;
  themeInitialized = true;
  applyTheme(nextTheme);
  try {
    window.localStorage.setItem("theme", nextTheme);
  } catch {}
  notifyThemeChange();
}

export function ThemeProvider({ children }) {
  const theme = useSyncExternalStore(subscribeTheme, getThemeSnapshot, getThemeServerSnapshot);

  const setTheme = useCallback((nextTheme) => {
    if (nextTheme !== "light" && nextTheme !== "dark") return;
    commitTheme(nextTheme);
  }, []);

  const toggleTheme = useCallback(() => {
    commitTheme(theme === "dark" ? "light" : "dark");
  }, [theme]);

  const value = useMemo(() => ({ theme, setTheme, toggleTheme }), [theme, setTheme, toggleTheme]);

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  return useContext(ThemeContext);
}
