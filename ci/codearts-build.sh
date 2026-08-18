#!/usr/bin/env bash

set -evx

export BUILD_ROOT=/opt/buildtools
export JAVA_HOME="${BUILD_ROOT}/bishengjdk-21.0.8"
export PATH="${JAVA_HOME}/bin:${PATH}"
DEVECO_ZIP="${WORKSPACE}/deveco-dependencies.zip"
DEVECO_DIR="${WORKSPACE}/deveco-dependencies"

export OBS_BASE_URL="https://cangjie103.obs.cn-north-4.myhuaweicloud.com"
DEVECO_URL="${OBS_BASE_URL}/DevecoPluginsBuild/${DEVECO_VERSION}/deveco-dependencies.zip"

rm -rf "${DEVECO_DIR}"
rm -f "${DEVECO_ZIP}"

wget --tries=3 \
    --timeout=60 \
    --output-document="${DEVECO_ZIP}" \
    "${DEVECO_URL}"

mkdir -p "${DEVECO_DIR}"
unzip -q "${DEVECO_ZIP}" -d "${DEVECO_DIR}"

DEVECO_HOME="$(dirname "$(find "${DEVECO_DIR}" \
    -type f \
    -name product-info.json \
    -print \
    -quit)")"

# ----------------- 本地拷贝 wrapper 并重写 URL -----------------
EXISTING_GRADLE_DIR="${WORKSPACE}/lsp-client/gradle"

if [ -d "${EXISTING_GRADLE_DIR}/wrapper" ]; then
    mkdir -p gradle
    cp -rf "${EXISTING_GRADLE_DIR}/wrapper" gradle/
else
    find "${WORKSPACE}" -maxdepth 5 -print
    exit 1
fi

chmod 644 gradle/wrapper/* 2>/dev/null || true

# 最简单直接：直接覆盖写入 distributionUrl，不用正则，不转义
echo "distributionUrl=${OBS_BASE_URL}/DevecoPluginsBuild/gradle-9.1.0-bin.zip" >> gradle/wrapper/gradle-wrapper.properties
# ---------------------------------------------------------------

chmod +x gradlew

./gradlew clean build \
    --stacktrace \
    --no-daemon \
    "-Pversion=${DEVECO_VERSION}" \
    "-PdevEcoHome=${DEVECO_HOME}"
