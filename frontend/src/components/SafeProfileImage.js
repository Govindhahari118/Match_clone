"use client";

import Image from "next/image";

export default function SafeProfileImage({
  src,
  alt = "Profile photo",
  width = 640,
  height = 760,
  sizes,
  className,
  style,
}) {
  if (!src) {
    return (
      <div
        className={className}
        role="img"
        aria-label="Photo unavailable"
        style={{
          width: style?.width || "100%",
          height: style?.height || "100%",
          minHeight: 96,
          display: "grid",
          placeItems: "center",
          background: "linear-gradient(135deg, #f1f5f9, #e2e8f0)",
          color: "#64748b",
          fontSize: "0.78rem",
          fontWeight: 700,
          ...style,
        }}
      >
        Photo unavailable
      </div>
    );
  }

  return (
    <Image
      src={src}
      alt={alt}
      width={width}
      height={height}
      sizes={sizes}
      className={className}
      style={style}
    />
  );
}
