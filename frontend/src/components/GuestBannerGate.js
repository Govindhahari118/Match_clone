"use client";

import { useSyncExternalStore } from "react";
import GuestBanner from "./GuestBanner";

function subscribe() {
  return () => {};
}

function getClientSnapshot() {
  return true;
}

function getServerSnapshot() {
  return false;
}

export default function GuestBannerGate() {
  const isClient = useSyncExternalStore(subscribe, getClientSnapshot, getServerSnapshot);
  if (!isClient) return null;
  return <GuestBanner />;
}
