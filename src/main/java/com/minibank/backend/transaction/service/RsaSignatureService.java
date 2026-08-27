package com.minibank.backend.transaction.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RsaSignatureService {

	public void verifyOrThrow(String publicKeyPem, String canonicalPayload, String signatureBase64) {
		if (publicKeyPem == null || publicKeyPem.isBlank()) {
			throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Public key is not registered");
		}
		if (signatureBase64 == null || signatureBase64.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signature is required");
		}

		try {
			PublicKey publicKey = parsePublicKey(publicKeyPem);
			byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);

			String algorithm = "EC".equalsIgnoreCase(publicKey.getAlgorithm())
				? "SHA256withECDSA"
				: "SHA256withRSA";
			if ("SHA256withECDSA".equals(algorithm) && sigBytes.length == 64) {
				sigBytes = rawEcdsaToDer(sigBytes);
			}

			Signature verifier = Signature.getInstance(algorithm);
			verifier.initVerify(publicKey);
			verifier.update(canonicalPayload.getBytes(StandardCharsets.UTF_8));

			boolean ok = verifier.verify(sigBytes);
			if (!ok) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid digital signature");
			}
		} catch (ResponseStatusException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid public key or signature: " + ex.getMessage());
		}
	}

	private static byte[] rawEcdsaToDer(byte[] rawSig) {
		byte[] r = Arrays.copyOfRange(rawSig, 0, 32);
		byte[] s = Arrays.copyOfRange(rawSig, 32, 64);

		r = toUnsignedDerInt(r);
		s = toUnsignedDerInt(s);

		int totalLen = 2 + r.length + 2 + s.length;
		byte[] der = new byte[2 + totalLen];
		int pos = 0;
		der[pos++] = 0x30;
		der[pos++] = (byte) totalLen;
		der[pos++] = 0x02;
		der[pos++] = (byte) r.length;
		System.arraycopy(r, 0, der, pos, r.length);
		pos += r.length;
		der[pos++] = 0x02;
		der[pos++] = (byte) s.length;
		System.arraycopy(s, 0, der, pos, s.length);
		return der;
	}

	private static byte[] toUnsignedDerInt(byte[] val) {
		int start = 0;
		while (start < val.length - 1 && val[start] == 0) {
			start++;
		}
		byte[] unsigned = Arrays.copyOfRange(val, start, val.length);
		if ((unsigned[0] & 0x80) != 0) {
			byte[] padded = new byte[unsigned.length + 1];
			padded[0] = 0x00;
			System.arraycopy(unsigned, 0, padded, 1, unsigned.length);
			return padded;
		}
		return unsigned;
	}

	private static PublicKey parsePublicKey(String pem) throws Exception {
		String normalized = pem
			.replace("-----BEGIN PUBLIC KEY-----", "")
			.replace("-----END PUBLIC KEY-----", "")
			.replaceAll("\\s+", "");

		byte[] der = Base64.getDecoder().decode(normalized);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(der);
		try {
			return KeyFactory.getInstance("EC").generatePublic(keySpec);
		} catch (GeneralSecurityException ignore) {
			return KeyFactory.getInstance("RSA").generatePublic(keySpec);
		}
	}
}
