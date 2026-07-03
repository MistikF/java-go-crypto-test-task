#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CERT_DIR="${ROOT_DIR}/certs"
PASS="${KEYSTORE_PASSWORD:-changeit}"
DAYS="3650"

mkdir -p "${CERT_DIR}/ca" "${CERT_DIR}/crypto" "${CERT_DIR}/tls" "${CERT_DIR}/client"
cd "${CERT_DIR}"

echo ">> Certificate Authority"
openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout ca/ca.key -out ca/ca.crt -days "${DAYS}" \
  -subj "/C=RU/O=Crypto Test Task/CN=Crypto Test Task Root CA"

issue() {
  local name="$1" subj="$2" san="$3" dir="$4"
  openssl req -newkey rsa:2048 -nodes \
    -keyout "${dir}/${name}.key" -out "${dir}/${name}.csr" -subj "${subj}"
  if [ -n "${san}" ]; then
    openssl x509 -req -in "${dir}/${name}.csr" \
      -CA ca/ca.crt -CAkey ca/ca.key -CAcreateserial \
      -out "${dir}/${name}.crt" -days "${DAYS}" \
      -extfile <(printf "subjectAltName=%s\nkeyUsage=digitalSignature,keyEncipherment,dataEncipherment\nextendedKeyUsage=serverAuth,clientAuth\n" "${san}")
  else
    openssl x509 -req -in "${dir}/${name}.csr" \
      -CA ca/ca.crt -CAkey ca/ca.key -CAcreateserial \
      -out "${dir}/${name}.crt" -days "${DAYS}" \
      -extfile <(printf "keyUsage=digitalSignature,keyEncipherment,dataEncipherment\nextendedKeyUsage=clientAuth,emailProtection\n")
  fi
  rm -f "${dir}/${name}.csr"
}

echo ">> Crypto identity (signing + encryption)"
issue crypto "/C=RU/O=Crypto Test Task/CN=crypto-identity" "" crypto

echo ">> Server TLS certificate"
issue server "/C=RU/O=Crypto Test Task/CN=localhost" "DNS:localhost,IP:127.0.0.1" tls

echo ">> Client certificate (mTLS)"
issue client "/C=RU/O=Crypto Test Task/CN=crypto-client" "" client

pack_p12() {
  local key="$1" crt="$2" out="$3" alias="$4"
  openssl pkcs12 -export -inkey "${key}" -in "${crt}" -certfile ca/ca.crt \
    -name "${alias}" -out "${out}" -passout "pass:${PASS}"
}

echo ">> PKCS#12 keystores"
pack_p12 crypto/crypto.key crypto/crypto.crt crypto/crypto.p12 crypto
pack_p12 tls/server.key    tls/server.crt    tls/server.p12    server
pack_p12 client/client.key client/client.crt client/client.p12 client

echo ">> JKS keystore (crypto identity)"
rm -f crypto/crypto.jks
keytool -importkeystore -noprompt \
  -srckeystore crypto/crypto.p12 -srcstoretype PKCS12 -srcstorepass "${PASS}" \
  -destkeystore crypto/crypto.jks -deststoretype JKS -deststorepass "${PASS}"

echo ">> Truststore (CA) for mTLS"
rm -f tls/truststore.p12
keytool -importcert -noprompt -alias ca \
  -file ca/ca.crt -keystore tls/truststore.p12 \
  -storetype PKCS12 -storepass "${PASS}"

echo
echo "Done. Keystore password: ${PASS}"
echo "Generated under: ${CERT_DIR}"
