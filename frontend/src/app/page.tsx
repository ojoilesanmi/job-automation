"use client";

import { AuthProvider } from "@/lib/auth-context";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Mail, Briefcase, Shield, Zap } from "lucide-react";

export default function Home() {
  return (
    <AuthProvider>
      <div className="flex min-h-screen flex-col">
        <header className="flex items-center justify-between border-b px-6 py-4">
          <div className="flex items-center gap-2">
            <Mail className="h-6 w-6 text-primary" />
            <span className="text-xl font-bold">JobAgent</span>
          </div>
          <div className="flex gap-3">
            <Link href="/auth/login"><Button variant="ghost">Sign in</Button></Link>
            <Link href="/auth/register"><Button>Get started</Button></Link>
          </div>
        </header>

        <main className="flex flex-1 flex-col items-center justify-center px-6 text-center">
          <h1 className="mb-4 text-5xl font-bold tracking-tight">
            Let AI handle your<br />job applications
          </h1>
          <p className="mb-8 max-w-xl text-lg text-muted-foreground">
            Automated job discovery, tailored cover letters, and application tracking — all in one platform.
          </p>
          <Link href="/auth/register">
            <Button size="lg" className="text-base">Start free</Button>
          </Link>
        </main>

        <section className="border-t bg-muted/40 px-6 py-16">
          <div className="mx-auto grid max-w-4xl gap-8 md:grid-cols-3">
            {[
              { icon: Briefcase, title: "Smart matching", desc: "AI scores every job against your profile and preferences." },
              { icon: Zap, title: "Cover letters in seconds", desc: "Generate tailored, high-quality cover letters per job." },
              { icon: Shield, title: "You stay in control", desc: "Review and approve every application before it goes out." },
            ].map(({ icon: Icon, title, desc }) => (
              <div key={title} className="rounded-lg border bg-card p-6 text-left shadow-sm">
                <Icon className="mb-3 h-8 w-8 text-primary" />
                <h3 className="mb-1 font-semibold">{title}</h3>
                <p className="text-sm text-muted-foreground">{desc}</p>
              </div>
            ))}
          </div>
        </section>

        <footer className="border-t px-6 py-4 text-center text-sm text-muted-foreground">
          Job Application Agent &copy; 2026
        </footer>
      </div>
    </AuthProvider>
  );
}
