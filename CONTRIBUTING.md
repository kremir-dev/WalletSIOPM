# Contributing to WalletSIOPM

Thank you for your interest in contributing to WalletSIOPM. Contributions from the open-source community are essential for maintaining and improving this financial management tool.

---

## Code of Conduct

This project is governed by our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to the repository maintainers.

---

## Legal Notice (License Agreement)

WalletSIOPM is distributed under the **GNU General Public License v3.0 (GPL-3.0)**. By submitting a Pull Request (PR) to this repository, you explicitly agree that your contributions will be licensed under the GPL-2.0 terms. You must hold the copyright for any code you submit, or possess the legal authority to license it under these terms.

---

## How to Contribute

### 1. Reporting Bugs
Before opening a new bug report, search the [Issue Tracker](https://github.com/kremir-dev/WalletSIOPM/issues) to verify that the bug has not been documented previously. 

When opening an issue, provide a comprehensive report containing:
* **Title:** A concise, definitive summary of the defect.
* **Reproduction Steps:** Explicit, sequential instructions to reproduce the behavior.
* **Behavior Analysis:** A clear contrast between the expected output and the actual outcome.
* **Environment Details:** Target Android API version (e.g., API 33 / Android 13) and hardware/emulator model.
* **Artifacts:** Attach relevant crash logs (Logcat output) or user interface screenshots.

### 2. Suggesting Enhancements
Feature requests must clearly define the utility of the modification. When opening an enhancement issue, please include:
* A structured functional specification of the proposed feature.
* Technical or user-experience justification for why this feature is required.
* Conceptual mockups, wireframes, or references to existing solutions where applicable.

### 3. Source Code Submissions (Pull Requests)

#### Workflow Strategy
1. **Fork the Repository:** Create a personal fork of the main repository.
2. **Branch Nomenclature:** Establish a feature branch using a specific naming convention:
   * For features: `feature/short-description`
   * For bug fixes: `fix/issue-number-description`
3. **Local Setup:** Clone your fork and open the directory inside Android Studio:
   ```bash
   git clone https://github.com/kremir-dev/WalletSIOPM.git
   cd WalletSIOPM
   ```

#### Technical and Style Guidelines
* **Language and Architecture:** Code implementations must be written in Java, utilizing AndroidX architectures and compliant Room persistence practices.
* **Code Formatting:** Adhere strictly to the standard Android Studio code style conventions. Ensure unused imports are optimized and code is cleanly indented prior to submission.
* **Commit Messages:** Commit messages must be informative and imperative. For example:
  `Fix database migration constraint failure for Room entities` instead of `fixed bug`.

#### Verification and Submission
* **Local Compilation:** The project must compile locally without errors or critical lint warnings. Ensure `gradlew build` executes successfully.
* **Data Integration:** If modifying Room database structures, verify that database migration schemas are explicitly handled to prevent runtime application crashes.
* **Pull Request Execution:** Submit your Pull Request against the `main` branch of the upstream repository. Ensure the PR description explicitly references any related issue tracking numbers (e.g., `Closes #12`).

---
*The project maintainers reserve the right to request architectural modifications, code revisions, or optimization changes before any Pull Request is approved and merged.*
