---
title: "CI secrets checklist for 0.1.0"
type: runbook
status: pending-secrets
---

# CI secrets checklist

`sbt-typelevel-ci-release` + the generated `.github/workflows/ci.yml`
expect the following GitHub repository secrets to be present before
a `v*` tag push will publish successfully. Until they're provisioned
the `publish` job fails cleanly with a credential error; the `build`
job (test + doc + mima) runs regardless.

Set each via **Settings → Secrets and variables → Actions → New
repository secret** on
<https://github.com/Constructive-Programming/eo>.

| Secret name                 | Value                                                                          |
|-----------------------------|--------------------------------------------------------------------------------|
| `SONATYPE_USERNAME`         | User token name from <https://central.sonatype.com/account>                    |
| `SONATYPE_PASSWORD`         | User token secret from the same page                                           |
| `SONATYPE_CREDENTIAL_HOST`  | `central.sonatype.com` (the new Portal; the legacy OSSRH host no longer works) |
| `PGP_SECRET`                | Base64-encoded ASCII-armoured private key — see "Signing key" below            |
| `PGP_PASSPHRASE`            | Passphrase on that key (optional — omit the secret if the key is passwordless) |

## Sonatype Central Portal namespace

`dev.constructive` must be registered and DNS-verified before the
first publish:

1. Log in to <https://central.sonatype.com> with the same Sonatype
   account.
2. **Publishing → Namespaces → Add Namespace** → enter
   `dev.constructive`.
3. Central Portal returns a DNS TXT record
   (`OSSRH-xxxxx`); add it to the `constructive.dev` authoritative
   DNS zone.
4. Wait for Central to verify (documentation says up to 48h; in
   practice often minutes). Refresh the Namespaces page — it
   flips to "Verified".
5. Generate a user token under **Account → Generate User Token**;
   the `name` becomes `SONATYPE_USERNAME`, the `password` becomes
   `SONATYPE_PASSWORD`.

Namespace-reuse gotcha: if `dev.constructive` was ever used under
legacy OSSRH, the Portal registration will bounce. Confirm the
coordinate is fresh; if in doubt, rename (e.g. `io.github.kryptt`).

## Signing key

```sh
gpg --full-generate-key          # RSA, 4096 bits, no expiry, "Constructive <release@constructive.dev>"
gpg --armor --export <KEY-ID>    # public key
gpg --keyserver keys.openpgp.org --send-keys <KEY-ID>
gpg --armor --export-secret-keys <KEY-ID> | base64 -w0 > pgp-secret.txt
```

`PGP_SECRET` is the contents of `pgp-secret.txt` pasted into the
GitHub secret.

Delete `pgp-secret.txt` after pasting.

## GitHub token (local / offline)

A fine-grained PAT is only needed for local scripts that poke the
GitHub REST API; CI itself uses the auto-injected
`secrets.GITHUB_TOKEN`. If you keep a long-lived PAT on your
machine, store it in `.envrc` (gitignored) as `GITHUB_TOKEN`,
**not** in this file.

> **Rotate any PAT** that has ever been pasted into an LLM chat,
> email, or commit message. Generate a replacement at
> <https://github.com/settings/tokens?type=beta> and update `.envrc`.

## Verification after tag push

```sh
git tag v0.1.0 && git push --tags
gh run watch                     # follow the publish workflow
```

Artefacts appear on Central Portal under
<https://central.sonatype.com/artifact/dev.constructive/cats-eo_3>
within minutes of the workflow's `publish` job succeeding.
