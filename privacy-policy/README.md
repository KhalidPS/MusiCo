# Privacy policy page

`index.html` is the privacy policy MusiCo links to from its Google Play listing. It is kept here,
next to the app, because it has to change whenever the app's permissions or SDKs change.

It is deliberately self-contained — no external CSS, JS, fonts or images — so it can be served
straight from GitHub Pages with no build step.

## Publishing

It is hosted from a **separate public repository**, not from this one. Free GitHub Pages only
serves public repos, and Pages can only publish from a repo's root or its `/docs` folder — and
`docs/` here holds internal roadmap and design notes that should not be public.

To update the live page, copy `index.html` into the hosting repo and push.

## Keeping it honest

The permission table must match the app's **merged** manifest, not just `AndroidManifest.xml` —
libraries inject permissions of their own. Firebase Analytics adds `AD_ID`,
`ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION` and
`BIND_GET_INSTALL_REFERRER_SERVICE`, which is why they appear in the policy even though nothing in
this codebase asks for them.

To re-check after changing dependencies or permissions, build a release and diff the two lists:

```bash
./gradlew :app:assembleRelease
grep -oE '<uses-permission[^>]*?name="[^"]*?\.([A-Z_]+)"' \
  app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml
```

Anything requested there and not explained in `index.html` needs adding. Component-level
`android:permission` attributes (`BIND_JOB_SERVICE`, `BIND_REMOTEVIEWS`, `DUMP`) are enforced on
the system rather than requested by the app, so they are correctly left out.

A policy that no longer matches the app is worse than none: Play checks it against the Data Safety
declaration, and a mismatch is a common cause of rejection.

## Not legal advice

This page describes what the app actually does, written from the manifest and source. It is not
legal advice. If MusiCo ever gains accounts, payments, or a meaningful EU/UK user base, have
someone qualified review it.
