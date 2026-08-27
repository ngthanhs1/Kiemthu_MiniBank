package com.minibank.backend.contract.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.minibank.backend.contract.entity.Contract;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(String ownerType, Long ownerId);

    List<Contract> findAllByOrderByCreatedAtDesc();

    @Query("select c from Contract c where c.ownerType = :ownerType " +
           "and c.ownerId = :ownerId and c.status <> 'CANCELLED'")
    List<Contract> findActiveByOwner(@Param("ownerType") String ownerType,
                                     @Param("ownerId") Long ownerId);

       Optional<Contract> findFirstByOwnerTypeAndOwnerIdAndStatus(
              String ownerType, Long ownerId, String status);

       boolean existsByTemplate_Id(Long templateId);
}

