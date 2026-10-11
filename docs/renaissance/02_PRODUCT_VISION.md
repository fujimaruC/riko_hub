# 02 — Product Vision

**Status:** Proposed product direction for the Renaissance roadmap. This document describes intent and user outcomes; it does not authorize implementation by itself. See [03_MASTER_ROADMAP.md](03_MASTER_ROADMAP.md) for accepted sequencing and [09_EXECUTION_BACKLOG.md](09_EXECUTION_BACKLOG.md) for implementation work.

## Why Riko Hub exists

Riko Hub helps a person capture something once, connect it to meaningful work, and act on it wherever they are. Classes, assignments, meetings, projects, notes, documents, and evidence should be findable through useful relationships instead of copied into disconnected lists. The user owns their records and can continue offline.

The product is a private, offline-first Android app built with Kotlin, Compose, and Room, with a small Python companion on Linux as an optional extension. Local capture, browsing, and task work must remain useful without an account, network, cloud service, or AI. The companion and future transfers extend this local workflow; they do not become a prerequisite for it.

Riko Hub is not a Notion clone or a general-purpose workspace builder. Its differentiation goal is practical continuity for this student’s work, with offline use and local ownership; it makes no claim to outperform another product. For example, capture a lab task on the phone between classes without a network, resume its linked desktop document on Linux at home, then attach the returned report as submission evidence. The task, file reference, and evidence remain understandable as one piece of work without requiring a cloud account.

## Who it serves

The primary user is a student balancing coursework, organization work, personal obligations, and longer projects. They need to know what is due, what belongs to a course, which document supports a task, and what to pick up after a break. The product should work on existing Android hardware, including Android 9, and tolerate intermittent connectivity.

The product must remain usable with accessibility services and larger text. Provide readable lists as alternatives to visual maps, predictable focus and TalkBack labels, sufficient contrast, and controls that do not depend on precise gestures. Playful shapes and gentle motion must not obscure urgency or legibility; reduced motion and large text remain first-class behavior.

## Core workflow

A user captures a task, note, event, or document reference where it occurs. They add the context that helps future retrieval: perhaps a course, project, deadline, or source document. The app then shows the item in a relevant list, deadline view, or relationship view. When the user completes work, they can attach evidence or record what happened. Later, they can return through the course, task, document, or history entry without remembering where the information was first entered.

The daily loop is simple: capture an item locally with only the fields needed now; use lists and backlinks to reach related work; follow the existing task and deadline cues; then resume on phone or Linux companion from an explicitly selected handoff.

## Product boundaries

The phone remains the dependable source for local work. A future Linux companion may receive selected context and files, but the app does not require live sync, shared editing, or continuous network. Google Drive integration has an accepted v0.3 direction; live quality assurance is unknown. The v0.4 companion source exists, but a remote source path remains proposed and does not establish a release contract.

Academic workflow and automation remain proposals. This document changes no schema, silently migrates no task status, and promises no specific device, network, LMS, notification, or calendar integration. New entities, protocols, and behaviors are **PROPOSED** until accepted in the roadmap.

## How to judge a feature

A proposal fits when it reduces repeated capture or searching, preserves offline usefulness and user control, works on supported Android hardware, and has an accessible default path. It must state stored data, edit and deletion behavior, failure handling, and what is guaranteed or unknown. Core value must not depend on cloud access, AI, or broad workspace configuration.

Related specifications: [06_SIGNATURE_FEATURES.md](06_SIGNATURE_FEATURES.md) defines proposed product concepts; [07_ACADEMIC_AND_AUTOMATION.md](07_ACADEMIC_AND_AUTOMATION.md) covers proposed academic states and local automation; [05_V05_SYNC_AND_HANDOFF.md](05_V05_SYNC_AND_HANDOFF.md) describes the later handoff direction; [08_ARCHITECTURE_AND_SECURITY.md](08_ARCHITECTURE_AND_SECURITY.md) and [09_EXECUTION_BACKLOG.md](09_EXECUTION_BACKLOG.md) define technical constraints and queued work.
