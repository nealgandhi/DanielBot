# Branches
Daniel Bot makes use of "feature branches" to allow multiple developers to work on separate features at the same time.
Each developer makes a branch, named starting with `feat/`, to work on their feature. For example, the FAQ system's
branch could be named `feat/faq-system`. These branches should be created off of `main`.

## What is a Branch?
Officially, a branch is a "pointer to a commit". In practice, branches can be thought of as a list of commits, starting
from the most recent commit in the branch and including all earlier commits in the project's history. When you create a
new branch, you actually just copy an old branch. Any new commits on the new branch will only move the new branch
forward, leaving the old branch untouched.

## How to use Branches
For example, let's say `main` has the commits `A`, `B`, and `C`. You want to implement the `X` feature. Start by
creating a feature branch for X: `git checkout -b feat/x main`. If you are already on `main`, you can leave it off:
`git checkout -b feat/x`. `git checkout -b` creates the new branch and automatically switches to it. Now, any future
commits will apply to `feat/x`, but not `main`. Let's say you add commits `X1`, `X2`, and `X3`. Here's what the
repository would look like, only showing `main` and `feat/x`:

```
commits:  A → B → C → X1 → X2 → X3
                  ↑              ↑
branches:       main          feat/x           
```

As you can see, `main` has does not know that `X1`, `X2`, and `X3` exist. This means you can push this branch without
fear of messing up anyone else's work, because they are also working on a separate branch off of `main`! To push this
branch, run `git push -u <remote> feat/x` (To find out what remote to use, run `git remote -v`. For example, if our repo
is under `origin`, you would run `git push -u origin feat/x`, without the <>). You only need to include
`-u <remote> feat/x` the first time; every time afterwards, `git push` will work fine (but accidentally including it
won't cause any problems).

## More Complicated Example
Let's say you're a different developer than the previous example, and want to work on the `Y` feature while `X` is
being developed. You create your `feat/y` as before, and add commits `Y1` and `Y2`. Here's what the repository would
look like, showing `main`, `feat/x`, and `feat/y`:

```
                       X1 → X2 → X3
commits:  A → B → C -<            ↑
                  ↑    Y1 → Y2    |
                  |          ↑    |
branches:       main     feat/y  feat/x
```

Now, the developer working on `feat/x` finishes their feature, squashes it into one commit, `X`, and merges it into
main. Now, the repository looks like this:

```
                 ┌-→ Y1 → Y2
commits: A → B → C → X     ↑
                     ↑     |
branches:          main   feat/y
```

This is a problem! Now, your branch can't be merged into `main` automatically. This is where the process of "squashing"
and "rebasing" come into play. Here's a [guide](/docs/rebase-and-squash.md) for how to do this. Let's say you squash
`Y1` and `Y2` into one commit, `Y`, and rebase onto `main`. Now, the repository looks like this:

```
commits: A → B → C → X → Y
                     ↑   ↑
branches          main   feat/y
```

Now, you can open your Pull Request and `feat/y` can be automatically merged into `main`!
