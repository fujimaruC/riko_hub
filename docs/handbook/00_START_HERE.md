# Welcome to Riko Hub

Riko Hub is a personal productivity command center built around an Android app and an optional Linux Companion. It brings tasks, schedules, study materials, and selected work handoffs closer together, while keeping local planning useful without a network connection.

> **A note on status:** source code, owner acceptance, and runtime verification are different kinds of evidence. The labels in this handbook keep those distinctions visible. See [current project status](../STATUS.md) before relying on a workflow.

## The problem we are solving

Personal work is often split between a task list, a calendar, course files, and whichever device happens to be nearby. That makes it easy to lose the context needed to continue. Riko Hub aims to make the path from capturing something to organizing it, acting on it, and resuming it later feel direct.

The product is shaped for academic and personal planning. The phone is the dependable place for local work; the Linux Companion extends selected workflows when useful. Cloud access, remote networking, and AI are not prerequisites for the core idea.

## A typical day

You might add a task between classes, check a daily view for what needs attention, open a course material from the Vault, and later send a chosen file or work context to a paired Linux laptop. Local tasks and schedules remain available when the network or Companion is not.

Some of that is already represented in source, while broader workflows remain in planning. In particular, current Course Space work is source-complete but unverified, remote Companion access is not implemented, and the wider Universal Handoff vision is only partly represented. The [feature catalog](01_FEATURES.md) gives the careful version-by-version account.

## How we make product decisions

- **Local first:** keep everyday capture and planning useful offline.
- **User control:** make sharing, external access, and data movement explicit.
- **Focused scope:** connect the workflows Riko Hub is built for rather than becoming a general-purpose workspace builder.
- **Practical performance:** favor clear, lightweight ways to find and act on work.
- **No required AI:** intelligence can be useful, but the product must not depend on it.
- **Evidence over labels:** a feature is not accepted just because its source exists.

Riko Hub is not a Notion clone. Its direction is a personal, offline-first bridge between academic planning, Android, and Linux workflows, with data ownership kept in view.

## What belongs here

Riko Hub focuses on personal tasks and planning, timelines and reminders, study materials, academic responsibilities as they are developed, connected devices, and deliberate file or context handoff. It does not promise shared live editing, automatic LMS submission, public remote access, or an AI assistant. Those boundaries help keep personal data and core behavior understandable.

## Find your way around

| If you want to… | Start here |
|---|---|
| See what exists and how certain its status is | [Feature catalog](01_FEATURES.md) |
| Understand future versions | [Roadmap](02_ROADMAP.md) |
| Follow Android, Drive, and Companion data paths | [Architecture](03_ARCHITECTURE.md) |
| Find code and resources | [Project structure](04_PROJECT_STRUCTURE.md) |
| Set up a development environment | [Getting started](05_GETTING_STARTED.md) |
| Make a focused contribution | [Contributing](06_CONTRIBUTING.md) |
| Understand the evolving visual direction | [Design direction](07_DESIGN_DIRECTION.md) |
| Decode product terms | [Glossary](08_GLOSSARY.md) |

For detailed plans and protocol contracts, see [Renaissance specifications](../renaissance/00_README.md), [Companion protocol](../V04_COMPANION_PROTOCOL.md), and [Handoff protocol](../V05_HANDOFF_PROTOCOL.md). Specifications describe intent; they are not proof that code exists.

## Where a new contributor can begin

First read the [status page](../STATUS.md), then pick a small task with a clear outcome. For code work, trace the existing path before editing: Android UI usually connects through a ViewModel and repository, while the Linux Companion has its own Python package. For product or design proposals, write down the user problem and the effect on offline use, privacy, accessibility, and recovery.

If a change affects architecture or visual direction, the Project Owner reviews that decision. A good first contribution can also be a documentation correction that makes an existing behavior easier to understand.

---

**Next:** [Features](01_FEATURES.md) · [Roadmap](02_ROADMAP.md) · [Getting started](05_GETTING_STARTED.md)
