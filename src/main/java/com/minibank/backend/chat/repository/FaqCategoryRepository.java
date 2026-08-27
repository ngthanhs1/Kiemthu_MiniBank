package com.minibank.backend.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.minibank.backend.chat.entity.FaqCategory;

@Repository
public interface FaqCategoryRepository extends JpaRepository<FaqCategory, Long> {
	List<FaqCategory> findByActiveTrueOrderBySortOrderAscNameAsc();
	List<FaqCategory> findAllByOrderBySortOrderAscNameAsc();
}
