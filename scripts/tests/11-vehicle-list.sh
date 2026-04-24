#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TOKEN="${TOKEN:-eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJjdXJsLXJlZ2lzdGVyLTE3NzY0MjgzMjhAbWFpbC5jb20iLCJ0aXBvIjoiQURNSU5JU1RSQURPUiIsImlzcyI6InNlbGYiLCJpZCI6IjdmZDUzYjM4LTA4MjMtNDNkYS1iZjUzLWZmNDdjMGM3NDVkNiIsImV4cCI6MTc3NjY4NzUyOCwiaWF0IjoxNzc2NDI4MzI4LCJlbWFpbCI6ImN1cmwtcmVnaXN0ZXItMTc3NjQyODMyOEBtYWlsLmNvbSJ9.diVeRHZN46Bsgq-A18SyB7oszOfZL0eOq79pGuqHzo6bamekfkc-NVGkXvu9uqLagB3saGgYAKMuiaiO2WiB7lhGqMeiCDM9M0HFNf1ahRomkIvd6MlD5O2WRLJbUZ_Nek3h-kMB-mHlIBvWWp4QvbWOBaCVcqEoA8V4ZicJkQPhRK-0vR2iS_8BK9QRVzIAFsS45YnvVfCfh4ba_8dwpJ_FDnJaijMUMLZ9eS6azjBWWIme3odPLmzLqCj-_EjqtH48nsCVcngI1pi3CCoSgnv4KeSAo5DJnV-Wrn74C-CkOxWYqS7diWAo0LXH3WQ5fyCXTuEax1SMoh2HqtqMTg}"

if [[ -z "${TOKEN}" ]]; then
  echo "Defina TOKEN para listar veículos."
  echo "Exemplo: TOKEN=<jwt> ./scripts/tests/11-vehicle-list.sh"
  exit 1
fi

curl -i -X GET "${BASE_URL}/api/veiculos" \
  -H "Authorization: Bearer ${TOKEN}"

echo
