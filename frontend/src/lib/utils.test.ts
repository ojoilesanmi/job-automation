import { parseSkills, parsePipeSeparated, cn } from "./utils";

describe("parseSkills", () => {
  it("returns empty array for null/undefined", () => {
    expect(parseSkills(null)).toEqual([]);
    expect(parseSkills(undefined)).toEqual([]);
  });

  it("returns empty array for empty string", () => {
    expect(parseSkills("")).toEqual([]);
  });

  it("splits comma-separated string", () => {
    expect(parseSkills("java, python, go")).toEqual(["java", "python", "go"]);
  });

  it("trims whitespace", () => {
    expect(parseSkills("  java ,  python  ")).toEqual(["java", "python"]);
  });

  it("filters empty entries", () => {
    expect(parseSkills("java,,python,")).toEqual(["java", "python"]);
  });

  it("passes through arrays", () => {
    expect(parseSkills(["java", "python"])).toEqual(["java", "python"]);
  });

  it("filters falsy values from arrays", () => {
    expect(parseSkills(["java", "", null as unknown as string, "go"])).toEqual(["java", "go"]);
  });
});

describe("parsePipeSeparated", () => {
  it("returns empty array for null", () => {
    expect(parsePipeSeparated(null)).toEqual([]);
  });

  it("splits pipe-separated string", () => {
    expect(parsePipeSeparated("Strong fit|Remote friendly")).toEqual(["Strong fit", "Remote friendly"]);
  });

  it("filters empty entries", () => {
    expect(parsePipeSeparated("a||b|")).toEqual(["a", "b"]);
  });
});

describe("cn", () => {
  it("merges class names", () => {
    const result = cn("text-red-500", "text-blue-500");
    expect(result).toBe("text-blue-500");
  });

  it("handles conditional classes", () => {
    const result = cn("base", false && "hidden", "extra");
    expect(result).toContain("base");
    expect(result).toContain("extra");
    expect(result).not.toContain("hidden");
  });
});
