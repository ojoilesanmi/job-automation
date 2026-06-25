import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function parseSkills(input: string | string[] | null | undefined): string[] {
  if (!input) return [];
  if (Array.isArray(input)) return input.filter(Boolean);
  return input.split(",").map((s) => s.trim()).filter(Boolean);
}

export function parsePipeSeparated(input: string | string[] | null | undefined): string[] {
  if (!input) return [];
  if (Array.isArray(input)) return input.filter(Boolean);
  return input.split("|").map((s) => s.trim()).filter(Boolean);
}
