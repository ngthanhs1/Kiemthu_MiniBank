package com.minibank.backend.account.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.minibank.backend.account.entity.AccountBalanceLedger;

public interface AccountBalanceLedgerRepository extends JpaRepository<AccountBalanceLedger, Long> {
	@Query("""
		select l
		from AccountBalanceLedger l
		left join fetch l.account a
		left join fetch a.user u
		left join fetch l.transaction t
		order by l.createdAt desc
	""")
	List<AccountBalanceLedger> findAllWithAccountAndTransaction();
}

