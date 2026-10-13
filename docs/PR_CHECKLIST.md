# PR Checklist

The developer completes these checks and records evidence in the PR. Complete review and QA confirmations before merging; mark an item N/A only with a reason.

## Developer checks

- [ ] The PR has a clear purpose, links to the work ticket, and describes the expected outcome, scope, and any known limitations.
- [ ] The changes are focused and have been self-reviewed. Unrelated changes, dead code, comments, debug statements, and temporary files are removed.
- [ ] There is no use of `any`; appropriate explicit or inferred types are used instead.
- [ ] Required build, lint, formatting, and automated test checks pass; CI is green.
- [ ] Test cases are written or updated and cover all identified scenarios, including normal behavior, edge cases, invalid input, failures.
- [ ] Stylesheets follow existing conventions and reuse shared styles and design tokens.
- [ ] Accessibility has been implemented in case of HTML changes
- [ ] Frontend PRs include before/after screenshots or a short recording of the UI changes
- [ ] The PR has been reviewed by reviewer

## QA checks

- [ ] The acceptance criteria are verified
- [ ] E2E tests are added or updated for affected user workflows
- [ ] Regression checks cover related features and integrations, not only the changed behavior.
- [ ] For UI changes, relevant browsers, screen sizes, keyboard navigation, accessibility, and loading, empty, and error states are checked.
- [ ] Manual and automated QA results are attached to the Ticket.