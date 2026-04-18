---
title: "Cloudflare Pages setup for the docs site"
type: runbook
status: pending-secrets
---

# Cloudflare Pages setup

The [`Deploy docs site`](../.github/workflows/deploy-site.yml)
workflow builds the Laika/mdoc site on every `push` to `main`
and every `v*` tag push, then deploys the generated HTML to
Cloudflare Pages via
[`cloudflare/wrangler-action`](https://github.com/cloudflare/wrangler-action).

Until the Cloudflare side is wired, the workflow fails with
"Authentication error" in the deploy step. Everything below
needs to happen **once**, before the first site push.

## 1. Create the Pages project

1. Log in to <https://dash.cloudflare.com>.
2. Pick the account that will host the docs.
3. **Workers & Pages → Pages → Create application → Direct upload**.
   Direct-upload, **not** Git integration — the `sbt docs/tlSite`
   build runs in GitHub Actions, and Cloudflare's build
   environment does not ship sbt / JDK 17.
4. Project name: **`cats-eo-docs`** (keep it alphanumeric and
   all-lowercase — Cloudflare requires it).
5. Upload *any* placeholder folder so the project exists. The
   workflow will overwrite it on the first run.
6. Note the production URL Cloudflare generates — it follows
   the form `https://cats-eo-docs.pages.dev`. Optionally attach
   a custom domain (e.g. `docs.constructive.dev`) under the
   project's **Custom domains** tab.

## 2. Generate an API token

1. **My profile → API tokens → Create token**.
2. Use the **Edit Cloudflare Workers** template. Under
   **Permissions**, keep:
   - `Account — Cloudflare Pages — Edit`
3. **Account resources**: scope to the account that owns the
   Pages project.
4. Leave **Zone resources** at the default (only needed for
   custom-domain management, which the workflow does not touch).
5. Create the token and copy it — Cloudflare only displays it
   once.

## 3. Find the Account ID

Any Cloudflare dashboard page shows the account ID in the
right-hand sidebar (16-character hex string). You can also grab
it with:

```sh
curl -s -H "Authorization: Bearer <API_TOKEN>" \
  https://api.cloudflare.com/client/v4/accounts | jq '.result[0].id'
```

## 4. Add the GitHub secrets + variable

On <https://github.com/Constructive-Programming/eo/settings/secrets/actions>:

| Kind     | Name                       | Value                                       |
|----------|----------------------------|---------------------------------------------|
| Secret   | `CLOUDFLARE_API_TOKEN`     | the token from step 2                       |
| Secret   | `CLOUDFLARE_ACCOUNT_ID`    | the account ID from step 3                  |
| Variable | `CLOUDFLARE_PAGES_PROJECT` | `cats-eo-docs` (override if you renamed it) |

Variables live under
<https://github.com/Constructive-Programming/eo/settings/variables/actions>
— if you leave the variable unset, the workflow falls back to
`cats-eo-docs`.

## 5. Trigger the first deploy

Either `git push origin main` or, from the Actions tab, run the
**Deploy docs site** workflow manually
(`workflow_dispatch`). Watch the job; on success the last line
of the deploy step prints the preview URL:

```
✨  Deployed cats-eo-docs to https://abc12345.cats-eo-docs.pages.dev
```

Production deployments (`v*` tag pushes) replace the
`https://cats-eo-docs.pages.dev` root.

## 6. Wire the API Scaladoc link (optional)

The site's Helium top-navigation "API" link currently uses the
plugin default. Point it at whichever host serves the Scaladoc
jars once the Central Portal release lands — typically
[javadoc.io](https://javadoc.io/):

```scala
// build.sbt (inside the `docs` project's settings)
ThisBuild / tlSiteApiUrl := Some(
  url("https://javadoc.io/doc/dev.constructive/cats-eo_3/latest/"),
)
```

## Troubleshooting

- **`Authentication error (code: 10000)`** — the API token
  lacks the `Cloudflare Pages — Edit` permission, or it's
  scoped to the wrong account.
- **`Project not found`** — the `CLOUDFLARE_PAGES_PROJECT`
  variable doesn't match an existing Pages project in the
  account the token is scoped to.
- **Stale content in production** — Cloudflare Pages caches
  aggressively. The `main` branch deploys to a preview URL; a
  `v*` tag (or a direct `--branch=main` + "Set as production"
  toggle in the dashboard) promotes to the production URL.
