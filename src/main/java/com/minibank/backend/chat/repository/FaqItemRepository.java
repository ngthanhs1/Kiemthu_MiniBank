package com.minibank.backend.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.minibank.backend.chat.entity.FaqItem;

@Repository
public interface FaqItemRepository extends JpaRepository<FaqItem, Long> {
	List<FaqItem> findByActiveTrueOrderByCreatedAtDesc();
	List<FaqItem> findByCategoryIdAndActiveTrueOrderByCreatedAtDesc(long categoryId);
	List<FaqItem> findByCategoryIdAndParentFaqItemIsNullAndActiveTrueOrderByCreatedAtDesc(long categoryId);
	List<FaqItem> findByCategoryIdOrderByCreatedAtDesc(long categoryId);
	List<FaqItem> findByParentFaqItemIdAndActiveTrueOrderByCreatedAtDesc(long parentFaqItemId);
	List<FaqItem> findByParentFaqItemIdOrderByCreatedAtDesc(long parentFaqItemId);
	long countByParentFaqItemIdAndActiveTrue(long parentFaqItemId);
	List<FaqItem> findAllByOrderByCreatedAtDesc();
	List<FaqItem> findByQuestionContainingIgnoreCaseOrderByCreatedAtDesc(String q);
}
