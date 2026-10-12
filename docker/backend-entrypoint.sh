#!/bin/sh
# Maintainer: Enzo Ribas (https://github.com/oEnzoRibas).
set -eu

fail() { printf '%s\n' "$1" >&2; exit 1; }
storage="${STORAGE_DIRECTORY:-/data/uploads}"
export STORAGE_DIRECTORY="$storage"
uid="$(id -u)"

if [ "$uid" = 0 ]; then
    [ "${IRR_INIT_STORAGE_PERMISSIONS:-false}" = true ] || fail 'Root startup requires explicit storage initialization opt-in.'
    [ "$storage" = /data/uploads ] || fail 'Storage initialization is restricted to /data/uploads.'
    [ ! -L "$storage" ] || fail 'Storage initialization refuses a symbolic link.'
    [ "$(realpath -m "$storage")" = /data/uploads ] || fail 'Storage initialization refuses a noncanonical path.'
    mkdir -p "$storage"
    chown 10001:10001 "$storage"
    chmod 0700 "$storage"
    exec setpriv --reuid=10001 --regid=10001 --init-groups --no-new-privs --bounding-set=-all /app/backend-entrypoint.sh "$@"
fi

[ "$uid" = 10001 ] && [ "$(id -g)" = 10001 ] || fail 'The backend must run as UID/GID 10001.'
[ -d "$storage" ] && [ -w "$storage" ] && [ -x "$storage" ] || fail 'Attachment storage is not writable by the backend user.'

# Preserve docker run IMAGE --spring-option=value as well as the default CMD.
case "${1:-}" in -*) set -- java -jar /app/irr.jar "$@" ;; esac
[ "$#" -gt 0 ] || set -- java -jar /app/irr.jar
exec "$@"
