#!/bin/sh
# Starts authenticated Redis without exposing its password in process arguments.
# Maintainer: Enzo Ribas (https://github.com/oEnzoRibas).
set -eu
: "${REDIS_PASSWORD:?Set REDIS_PASSWORD in the service secret configuration.}"
if [ "${#REDIS_PASSWORD}" -lt 32 ]; then
  echo 'REDIS_PASSWORD must contain at least 32 base64url characters.' >&2
  exit 1
fi
case "$REDIS_PASSWORD" in
  *[!A-Za-z0-9_-]*) echo 'REDIS_PASSWORD must use the base64url alphabet.' >&2; exit 1 ;;
esac
# Railway mounts a root-owned filesystem containing lost+found. The upstream
# entrypoint deliberately skips recursive permission repair when unknown files exist.
# Initialize only the fixed mount root; never change unrelated filesystem contents.
[ ! -L /data ] || { echo 'Redis refuses a symbolic-link data mount.' >&2; exit 1; }
if [ "$(id -u)" = 0 ]; then
  chown redis:redis /data
  chmod 0700 /data
fi
umask 077
config=$(mktemp /tmp/irr-redis.XXXXXX)
printf 'dir /data\nappendonly yes\nappendfsync everysec\nmaxmemory 64mb\nmaxmemory-policy noeviction\nprotected-mode yes\nrequirepass "%s"\n' "$REDIS_PASSWORD" > "$config"
chown redis:redis "$config"
unset REDIS_PASSWORD
# Calling the retained upstream entrypoint with redis-server drops to its Redis user.
exec /usr/local/bin/docker-entrypoint.sh redis-server "$config"
