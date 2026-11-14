# Separately installable custom Nightjar APK

The dedicated GitHub Actions workflow produces an optimized, non-debuggable Android APK:

- **Package ID:** `com.butterpollo.client.custom`
- **Launcher name:** Nightjar (no Debug suffix)
- **Build:** R8-minified, arm64-only, based on the current upstream version integrated into this fork
- **Installation:** alongside the official `com.butterpollo.client` without changing its settings

## One-time setup entirely through GitHub

1. Open the repository **Settings → Secrets and variables → Actions**.
2. Add a **repository secret** named `CUSTOM_NIGHTJAR_SIGNING_PASSWORD`, containing a unique, randomly generated password of at least **24 characters**. Keep an independent secure copy in a password manager. Never put this value in a workflow input, commit, issue, Actions log or chat.
3. After the workflow has been merged into `main`, go to **Actions → Nightjar custom APK → Run workflow** and select `main`.

The first run creates a persistent 3072-bit RSA signing key in a PKCS#12 keystore inside the Actions runner. The keystore is doubly encrypted: PKCS#12 password protection and AES-256-CBC with PBKDF2 (600,000 iterations). The encrypted archive, **not** the password or plaintext keystore, is stored in a dedicated repository branch `custom-signing-key` containing only `nightjar-custom.p12.enc`. Subsequent runs decrypt that same key using the GitHub secret and can update the custom Nightjar installation in place.

**Security tradeoff:** The encrypted keystore is stored in this repository and may be publicly downloadable when the repository is public. Its security depends on your secret's strength and continued confidentiality. Back up both the encrypted branch and the password securely outside GitHub. Deleting or losing either makes future settings-preserving custom APK updates impossible. Anyone with both the branch contents and the secret password could sign malicious updates; restrict secret access and workflow modification accordingly.

Do not delete `custom-signing-key` or rotate the password without re-encrypting its keystore. Ordinary GitHub workflow runs use only the encrypted file and secret; signing material is removed from the runner afterward.

## APK installation

When the workflow passes, download its `nightjar-custom-arm64` Actions artifact and extract
the versioned `nightjar-<version>-custom-arm64.apk`. Installing it creates a **second Nightjar**, independent of the official app. You will need to configure and pair the new app once. Future builds that retain the package name and this signing key can update it without losing its settings, provided their version codes are compatible.

The official Nightjar 4.12.4 installation is not modified by installing this custom APK.

### Build validation

A separate Android debug CI workflow exercises `:app:assembleNonRootCustom` with a temporary validation certificate. This **does not** create a final installable artifact; the dedicated `Nightjar custom APK` workflow uses your persistent signing identity for that.
