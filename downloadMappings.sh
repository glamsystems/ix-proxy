#!/usr/bin/env bash

set -e

git clone -n --depth=1 --filter=tree:0 https://github.com/glamsystems/glam-sdk.git glam
cd glam
git sparse-checkout set --no-cone /remapping
git checkout

cd ..

exit 0
