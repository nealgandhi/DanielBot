# Contributing
Daniel Bot is a community project, and we welcome your contributions! Here's a guide on how to contribute.

## Setting Up
See the [setup guide](/docs/setup.md) for how to set up Daniel Bot.

## Commit Style
Daniel Bot follows [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

This means:

- All commits should have a prefix categorizing what they add (`feat:` for a feature, `fix:` for a 
  bug fix, `docs:` for documentation, etc).
- After the prefix, don't use a capital letter.
  - For example, `feat: implement faq system`.
- Use imperative phrasing, as if you were completing the sentence "this commit will".
  - For example, "this commit will implement faq system"
  - It's OK to leave out articles like "a", "an", and "the".
- If a commit is simple, it's OK to have only a single line, but more complex commits should have a description on the
  following lines. Always leave an empty line between the summary and description.
  - For example:
```
fix: correct minor typos in code

see the issue for details on typos fixed.
```
- The first line (the subject line) should always be 50 characters or less, and all lines after (the body) should be 72
  characters or less.
  - This can be configured in IDEA, under "Version Control", then "Commit" in Settings.

## Submitting Changes
Most of Daniel Bot's work is done on feature branches. See [here](/docs/branches.md) for an explanation of this.

Features should be implemented as KordEx [`Extension`](https://kordex.netlify.app/latest/)s.

Once a feature is finished and ready to be integrated into Daniel Bot, you should squash your commits if there are a
lot, and rebase the branch onto `main`. Instructions on how to do this are available
[here](/docs/rebase-and-squash.md). Once the branch is rebased, open a Pull Request to merge your feature branch
into `main`. All GitHub Actions must pass before a PR will be merged.
