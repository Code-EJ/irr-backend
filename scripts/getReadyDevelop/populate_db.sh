#!/usr/bin/env bash
# Compatibility entrypoint. Maintainer: Enzo Ribas (https://github.com/oEnzoRibas).
# Creates retained workflow fixtures through the authorized API, not direct SQL.
set -euo pipefail
cd "$(dirname "$0")/../.."
node scripts/smoke-development.mjs
