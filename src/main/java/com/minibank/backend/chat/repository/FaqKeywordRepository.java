package com.minibank.backend.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.minibank.backend.chat.entity.FaqKeyword;

@Repository
public interface FaqKeywordRepository extends JpaRepository<FaqKeyword, Long> {
	List<FaqKeyword> findByFaqItemId(long faqItemId);
	void deleteByFaqItemId(long faqItemId);
	List<FaqKeyword> findByNormalizedKeyword(String normalizedKeyword);
	List<FaqKeyword> findByNormalizedKeywordContaining(String fragment);
}
