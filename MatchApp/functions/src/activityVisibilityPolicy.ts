export type ActivityVisibility =
  "everyone" | "interests" | "mutual" | "nobody";

export type ActivityRelationship = {
  interested: boolean;
  mutual: boolean;
};

export function normalizeActivityVisibility(value: unknown): ActivityVisibility {
  return value === "everyone" ||
    value === "interests" ||
    value === "mutual" ||
    value === "nobody" ?
    value :
    "mutual";
}

export function activityVisibilityAllows(
  choice: ActivityVisibility,
  state: ActivityRelationship
): boolean {
  switch (choice) {
  case "everyone":
    return true;
  case "interests":
    return state.interested;
  case "mutual":
    return state.mutual;
  case "nobody":
    return false;
  }
}
