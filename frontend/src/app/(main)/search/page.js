import { redirect } from "next/navigation";

export default async function SearchPageRedirect({ searchParams }) {
  const resolved = (await searchParams) || {};
  const params = new URLSearchParams();

  Object.entries(resolved).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      value.forEach((entry) => {
        if (entry !== undefined && entry !== null && String(entry).trim() !== "") {
          params.append(key, String(entry));
        }
      });
      return;
    }

    if (value !== undefined && value !== null && String(value).trim() !== "") {
      params.set(key, String(value));
    }
  });

  const query = params.toString();
  redirect(query ? `/matches?${query}` : "/matches");
}
