"use client";

import { createContext, useCallback, useContext, useMemo, useSyncExternalStore } from "react";

const SUPPORTED_THEMES = ["glacier", "light", "dark", "lavender", "solar", "rose"];
const DEFAULT_THEME = "glacier";

const ThemeContext = createContext({
  theme: DEFAULT_THEME,
  setTheme: () => {},
  toggleTheme: () => {},
});

const themeListeners = new Set();
let themeSnapshot = DEFAULT_THEME;
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
    if (stored && SUPPORTED_THEMES.includes(stored)) return stored;

    const prefersDark = window.matchMedia?.("(prefers-color-scheme: dark)")?.matches;
    return prefersDark ? "dark" : DEFAULT_THEME;
  } catch {
    return DEFAULT_THEME;
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
  return DEFAULT_THEME;
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
    if (!SUPPORTED_THEMES.includes(nextTheme)) return;
    commitTheme(nextTheme);
  }, []);

  const toggleTheme = useCallback(() => {
    const currentThemeIndex = SUPPORTED_THEMES.indexOf(theme);
    const nextTheme = SUPPORTED_THEMES[(currentThemeIndex + 1) % SUPPORTED_THEMES.length];
    commitTheme(nextTheme || DEFAULT_THEME);
  }, [theme]);

  const value = useMemo(() => ({ theme, setTheme, toggleTheme }), [theme, setTheme, toggleTheme]);

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  return useContext(ThemeContext);
}
