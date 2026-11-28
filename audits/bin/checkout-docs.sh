#!/usr/bin/env bash
# Usage: audits/bin/checkout-docs.sh [--update] <docs-dir> [<revision>]
#
# Checks out the mongodb/docs pages the audits read: a shallow, blobless, sparse checkout of the
# source bundle's roots (audits/config/manual-baseline-sources.json), at <revision>. The revision
# defaults to the commit that file pins; HEAD is the head of the docs' default branch.
#
# Without --update, <docs-dir> must not exist yet. With --update, it must be a checkout this script
# made: it fetches <revision> and moves the checkout to it, so a bumped pin needs no new clone.
set -euo pipefail

usage() { sed -n 2,9p "$0" >&2; exit 2; }
update=
if [ "${1:-}" = "--update" ]; then update=1; shift; fi
[ $# -ge 1 ] && [ $# -le 2 ] || usage
docs=$1
config="$(cd "$(dirname "$0")/.." && pwd)/config/manual-baseline-sources.json"

read_config() { python3 -I -c 'import json,sys; print(json.load(open(sys.argv[1]))[sys.argv[2]])' "$config" "$1"; }
revision=${2:-$(read_config revision)}
uri=$(read_config uri)
# The bundle reads only the roots and the symlink targets it may follow. Without a trailing slash a
# non-cone pattern matches a file, a directory or a symlink at that path.
mapfile -t patterns < <(python3 -I -c '
import json, sys
request = json.load(open(sys.argv[1]))
for root in sorted(set(request["roots"] + request["allowedSymlinkRoots"])):
    print("/" + root)' "$config")

if [ -n "$update" ]; then
    [ -d "$docs/.git" ] || { echo "$docs is not a docs checkout; run without --update" >&2; exit 1; }
    [ -z "$(git -C "$docs" status --porcelain)" ] || { echo "$docs has local changes" >&2; exit 1; }
else
    [ ! -e "$docs" ] || { echo "$docs already exists; use --update" >&2; exit 1; }
    mkdir -p "$(dirname "$docs")"
    git init -q "$docs"
    git -C "$docs" remote add origin "$uri"
    git -C "$docs" config remote.origin.promisor true
    git -C "$docs" config remote.origin.partialclonefilter blob:none
fi

git -C "$docs" sparse-checkout set --no-cone "${patterns[@]}"
git -C "$docs" fetch -q --depth=1 --filter=blob:none --no-tags origin "$revision"
git -C "$docs" checkout -q --detach FETCH_HEAD
echo "$docs is at mongodb/docs $(git -C "$docs" rev-parse HEAD)"
