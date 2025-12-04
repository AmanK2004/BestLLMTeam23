README.md — BestLLMTeam23

BestLLMTeam23 – USC LLM Sharing App
CSCI 310 • Software Engineering • Team 23 (Jobless Dreamers)

Improvements Since Project 2.4 (Sprint 2.5 Enhancements)

During Sprint 2.5, our team implemented significant functionality upgrades, architectural improvements, and UX refinements beyond the capabilities delivered in Project 2.4. These additions were based on missing requirements identified by the customer (TA) as well as new competitive features requested during user-feedback discussions.

The major improvements delivered in this sprint include:

1. Enhanced State Management & Page Transition Stability

We redesigned state handling across the app to eliminate data loss and provide a smoother, more professional user experience.

Improvements:

Preserved scroll position when switching between major tabs.

Persisted search filters, sort preferences, and form inputs across navigation.

Implemented ViewModel + SavedStateHandle architecture for consistent state restoration.

Eliminated stale content caused by desynchronized Room database reads/writes.

Reduced UI lag and jitter during screen transitions.

Result: Navigation now feels stable, predictable, and polished, with significantly fewer user frustrations.

2. Version History Tracking for Posts, Prompts, and Comments

We added a complete content versioning system to match competitor platforms and improve content transparency.

Improvements:

Database schema now stores multiple versions of edited posts, prompts, and comments.

UI entry points allow users to open version history from any edited item.

Each version includes timestamps and sequential version labels.

Added foundations for future diff-view functionality.

Result: Users can review and trust the evolution of shared content, improving platform reliability and accountability.

3. Bookmarks & Saved Content Library

We introduced a new engagement feature allowing users to save meaningful content for later.

Improvements:

Added one-tap bookmarking for posts and prompts.

Built a dedicated Saved Library screen.

Added filters for content type, tags, and LLM categories.

Synced bookmarks with Room persistence for app-restart consistency.

Result: Users can easily revisit helpful posts and prompts, improving long-term engagement.

4. Drafts & Anonymous Posting for Prompts

Project 2.4 supported drafting/anonymous posting only for discussion posts; this sprint extended the same workflow to prompts.

Improvements:

Added a Drafts tab for prompts, mirroring the functionality for posts.

Implemented anonymous prompt publishing options.

Ensured draft prompts are private and invisible to community feeds.

Preserved existing private-post behavior.

Result: Users can develop prompt ideas more freely and publish anonymously when desired.

5. Fuzzy Search for Post Full-Text Search

We improved the discoverability of posts by adding a dynamic fuzzy-search algorithm.

Improvements:

Developed a DP-based fuzzy-matching algorithm to detect partial terms and near-matches.

Supports substring detection of long technical terms (e.g., “ThisIsAVeryLongTechnicalTerm”).

Added UX improvements including highlighting matched text in search results.

Integrated tuning options to reduce noise and prevent inaccurate matches.

Result: Search now feels flexible and intelligent, returning relevant results even with incomplete terms or typos.

6. Additional UI, Testing, and Stability Enhancements

Beyond feature additions, we strengthened internal architecture and reliability:

Improvements:

Added unit tests, instrumentation tests, and regression tests for new code paths.

Improved UI responsiveness across multiple screens.

Conducted performance profiling on long-body fuzzy searches.

Reduced race conditions through improved coroutine and repository design.

Result: The application is more robust, more testable, and closer to production-ready quality.





Project 2.4 Description

Overview

BestLLMTeam23 is an Android application that allows USC students to:

Create and share LLM-generated posts

Create, edit, and manage AI prompts

Search by tags, prompts, and users

Vote and comment on posts

Manage profiles, bios, departments, and visibility settings

This README explains how to:

Build and run the application

Execute black-box instrumentation tests

Execute white-box JUnit unit tests

Run test coverage for the white-box tests

1. Project Structure
BestLLMTeam23/
│
├── app/
│   ├── src/
│   │   ├── main/                  # Application source code
│   │   ├── androidTest/           # Black-box (Espresso) tests
│   │   └── test/                  # White-box (JUnit) tests
│   └── build.gradle
│
├── gradle/                        # Gradle wrapper
├── README.md                      # This file
└── settings.gradle

2. Requirements

Android Studio Flamingo / Hedgehog or later

Android SDK 33 or 34

Gradle Wrapper (included)

Android Emulator (Pixel 6 or Pixel 7 recommended)

3. How to Run the Application

Open Android Studio

Select File → Open

Choose the project root folder:

BestLLMTeam23/


Sync the project with Gradle (Android Studio prompts automatically)

Set up an emulator:

Tools → Device Manager

Create a device (Pixel 6, API 33/34 recommended)

Click the green Run ▶️ button to build & launch the app on the emulator.

4. How to Run Black-Box Tests (Espresso Instrumented Tests)

Black-box tests live in:

app/src/androidTest/java/com/example/csci310team23/


These tests require an emulator or physical device.

Steps:
A. Prepare the project

Clean Project

Invalidate Caches & Restart

Sync Project with Gradle

B. Start an emulator

Launch Pixel 6/7 (API 33/34)

C. Run all black-box tests

From Android Studio terminal:

./gradlew assembleDebugAndroidTest --stacktrace
./gradlew connectedDebugAndroidTest --stacktrace


This will:

Build the instrumentation test APK

Install both app + test APKs

Run all Espresso tests

Display results in the Test Runner panel

D. Run a single test

Right-click the test method → Run 'testName'

5. How to Run White-Box Tests (JUnit Unit Tests)

White-box tests live in:

app/src/test/java/com/example/csci310team23/


These tests run locally on the JVM (no emulator required).

Run all white-box tests:
./gradlew test --rerun-tasks

Run individual white-box tests

Right-click a test file → Run 'RoomAppRepositoryWhiteBoxTest'

6. How to Run Coverage (White-Box)

Open the test file:

RoomAppRepositoryWhiteBoxTest.kt


Right-click → Run '…' with Coverage

When prompted, choose:
Replace Active Suites

View coverage report:

Bottom-left panel → Coverage

Expand packages to view per-file line coverage

(Optional) Repeat for:

SearchStateWhiteBoxTest.kt

7. Black-Box Test File Locations

app/src/androidTest/java/com/example/csci310team23/FeedInteractionsTest.kt

app/src/androidTest/java/com/example/csci310team23/AuthProfileTest.kt

Additional test classes under the same directory

8. White-Box Test File Locations

app/src/test/java/com/example/csci310team23/RoomAppRepositoryWhiteBoxTest.kt

app/src/test/java/com/example/csci310team23/SearchStateWhiteBoxTest.kt

9. Notes & Troubleshooting
Espresso tests failing due to UI animations or race conditions

Cold boot the emulator

Disable animations in Developer Options

Gradle sync errors

File → Invalidate Caches

Re-sync

Coverage panel not visible

View → Tool Windows → Coverage

10. Authors (Team 23 — Jobless Dreamers)
Name	Email
Jie Chen	jchen359@usc.edu

Terry Tao	terryt@usc.edu

Nora Sun	ningyues@usc.edu

Aman Kumar	amankuma@usc.edu

Warren Wang	wangwarr@usc.edu
