import { render, screen } from "@testing-library/react";
import React from "react";
import { Sidebar } from "./sidebar";

jest.mock("next/navigation", () => ({
  usePathname: () => "/dashboard/overview",
}));

jest.mock("@/lib/auth-context", () => ({
  useAuth: () => ({
    user: { firstName: "John", lastName: "Doe", email: "john@example.com" },
    logout: jest.fn(),
    hasPermission: () => false,
  }),
}));

jest.mock("next/link", () => {
  return React.forwardRef(function MockLink(
    { children, href, ...props }: { children: React.ReactNode; href: string; [key: string]: unknown },
    ref: React.Ref<HTMLAnchorElement>
  ) {
    return React.createElement("a", { href, ref, ...props }, children);
  });
});

describe("Sidebar", () => {
  it("renders the app name", () => {
    render(<Sidebar />);
    expect(screen.getByText("JobAgent")).toBeInTheDocument();
  });

  it("renders all main nav items", () => {
    render(<Sidebar />);
    const expectedItems = [
      "Overview", "My Profile", "Preferences", "Jobs", "Matches",
      "Applications", "Cover Letters", "Templates", "Notifications", "Skill Gaps",
    ];
    for (const item of expectedItems) {
      expect(screen.getByText(item)).toBeInTheDocument();
    }
  });

  it("renders user info", () => {
    render(<Sidebar />);
    expect(screen.getByText("John Doe")).toBeInTheDocument();
    expect(screen.getByText("john@example.com")).toBeInTheDocument();
  });

  it("renders sign out button", () => {
    render(<Sidebar />);
    expect(screen.getByText("Sign out")).toBeInTheDocument();
  });

  it("highlights the active route", () => {
    render(<Sidebar />);
    const overviewLink = screen.getByText("Overview").closest("a");
    expect(overviewLink?.className).toContain("bg-accent");
  });

  it("does not show admin section for non-admin users", () => {
    render(<Sidebar />);
    expect(screen.queryByText("Admin")).not.toBeInTheDocument();
  });

  it("shows admin section for users with permissions", () => {
    const authModule = require("@/lib/auth-context");
    authModule.useAuth = () => ({
      user: { firstName: "Admin", lastName: "User", email: "admin@example.com" },
      logout: jest.fn(),
      hasPermission: () => true,
    });

    const { unmount } = render(<Sidebar />);
    expect(screen.getByText("Admin")).toBeInTheDocument();
    expect(screen.getByText("Users")).toBeInTheDocument();
    expect(screen.getByText("Audit Logs")).toBeInTheDocument();
    unmount();
  });
});
