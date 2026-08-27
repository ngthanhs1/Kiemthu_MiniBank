package com.minibank.backend.transaction.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.minibank.backend.transaction.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
	Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

	Optional<Transaction> findByIdAndInitiatedByUserId(Long id, Long initiatedByUserId);

	@Query("""
		select t
		from Transaction t
		left join fetch t.fromAccount fromAccount
		left join fetch fromAccount.user fromUser
		left join fetch t.toAccount toAccount
		left join fetch toAccount.user toUser
		where fromUser.id = :userId or toUser.id = :userId
		order by t.createdAt desc
	""")
	List<Transaction> findAllForUser(@Param("userId") Long userId);

	@Query("""
		select t
		from Transaction t
		left join fetch t.fromAccount fromAccount
		left join fetch fromAccount.user fromUser
		left join fetch t.toAccount toAccount
		left join fetch toAccount.user toUser
		where (fromUser.id = :userId or toUser.id = :userId)
		  and (coalesce(:fromTime, t.createdAt) = t.createdAt or t.createdAt >= :fromTime)
		  and (coalesce(:toTime, t.createdAt) = t.createdAt or t.createdAt <= :toTime)
		order by t.createdAt desc
	""")
	List<Transaction> findForUserWithDateFilter(
		@Param("userId") Long userId,
		@Param("fromTime") Instant fromTime,
		@Param("toTime") Instant toTime,
		Pageable pageable
	);

	@Query("""
		select t
		from Transaction t
		left join fetch t.fromAccount fromAccount
		left join fetch fromAccount.user fromUser
		left join fetch t.toAccount toAccount
		left join fetch toAccount.user toUser
		where t.id = :transactionId
		  and (fromUser.id = :userId or toUser.id = :userId)
	""")
	Optional<Transaction> findAccessibleByIdAndUserId(@Param("transactionId") Long transactionId, @Param("userId") Long userId);

	Optional<Transaction> findByQrTransferIntentId(Long qrTransferIntentId);

	long countByStatus(String status);

	@Query("select t from Transaction t where (t.fromAccount.user.id = :userId or t.toAccount.user.id = :userId) order by t.createdAt desc")
	List<Transaction> findRecentForUser(@Param("userId") Long userId, Pageable pageable);

	@Query("select t from Transaction t where t.status = 'completed' and t.createdAt >= :from order by t.createdAt desc")
	List<Transaction> findCompletedSince(@Param("from") java.time.Instant from);

	@Query("""
		select t
		from Transaction t
		left join fetch t.fromAccount fromAccount
		left join fetch fromAccount.user fromUser
		left join fetch t.toAccount toAccount
		left join fetch toAccount.user toUser
		order by t.createdAt desc
	""")
	List<Transaction> findAllWithAccounts();

	@Query("""
		select t
		from Transaction t
		left join fetch t.fromAccount fromAccount
		left join fetch fromAccount.user fromUser
		left join fetch t.toAccount toAccount
		left join fetch toAccount.user toUser
		where t.createdAt >= :from
		order by t.createdAt desc
	""")
	List<Transaction> findAllWithAccountsSince(@Param("from") Instant from);

	@Query("select count(t) from Transaction t where t.status in :statuses and t.amount >= :minAmount")
	long countByStatusInAndAmountGreaterThanEqual(
		@Param("statuses") List<String> statuses,
		@Param("minAmount") BigDecimal minAmount
	);

	@Query("select t from Transaction t "
		+ "join fetch t.fromAccount fa "
		+ "join fetch fa.user fu "
		+ "join fetch t.toAccount ta "
		+ "join fetch ta.user tu "
		+ "where t.status in :statuses "
		+ "and t.amount >= :minAmount "
		+ "order by t.createdAt desc")
	List<Transaction> findLargePending(
		@Param("statuses") List<String> statuses,
		@Param("minAmount") java.math.BigDecimal minAmount
	);

	@Query("select t from Transaction t "
		+ "where t.initiatedByUser.id = :userId "
		+ "and t.status in :statuses "
		+ "order by t.createdAt desc")
	List<Transaction> findPendingForUser(
		@Param("userId") Long userId,
		@Param("statuses") List<String> statuses,
		Pageable pageable
	);
}
