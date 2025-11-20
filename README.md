README.md — BestLLMTeam23
Project Title

BestLLMTeam23 – USC LLM Sharing App
CSCI 310 • Software Engineering • Team 23 (Jobless Dreamers)

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
