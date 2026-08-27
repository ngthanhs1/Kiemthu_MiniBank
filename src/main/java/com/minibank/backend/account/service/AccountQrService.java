package com.minibank.backend.account.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.entity.AccountQrCode;
import com.minibank.backend.account.repository.AccountQrCodeRepository;

@Service
public class AccountQrService {
	private final AccountQrCodeRepository qrCodeRepository;

	public AccountQrService(AccountQrCodeRepository qrCodeRepository) {
		this.qrCodeRepository = qrCodeRepository;
	}

	@Transactional
	public String getOrCreateActivePayload(Account account) {
		if (account == null || account.getId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is required");
		}

		Optional<AccountQrCode> existing = qrCodeRepository.findFirstByAccountIdAndActiveTrueOrderByCreatedAtDesc(account.getId());
		if (existing.isPresent()) {
			return existing.get().getQrPayload();
		}

		String payload = buildPayload(account);
		AccountQrCode qr = AccountQrCode.builder()
			.account(account)
			.qrPayload(payload)
			.qrImageUrl(null)
			.active(true)
			.createdAt(Instant.now())
			.build();
		qrCodeRepository.save(qr);
		return payload;
	}

	private static String buildPayload(Account account) {
		String accNo = account.getAccountNumber();
		String accName = account.getAccountName();
		// Keep as a simple JSON payload for easy scanning/parsing on client.
		String safeName = accName == null ? "" : accName.replace("\"", "\\\"");
		return "{\"bank\":\"MiniBank\",\"accountNumber\":\"" + accNo + "\",\"accountName\":\"" + safeName + "\"}";
	}
}
