package com.wholesale.repository;

import com.wholesale.model.CustomerPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerPriceRepository extends JpaRepository<CustomerPrice, Long> {

    List<CustomerPrice> findByCustomerIdOrderByEffectiveDateDesc(Long customerId);

    List<CustomerPrice> findByProductIdOrderByEffectiveDateDesc(Long productId);

    List<CustomerPrice> findByCustomerIdAndProductIdOrderByEffectiveDateDesc(Long customerId, Long productId);

    @Query("SELECT cp FROM CustomerPrice cp WHERE cp.customerId = :customerId AND cp.productId = :productId " +
           "ORDER BY cp.effectiveDate DESC, cp.createdAt DESC LIMIT 1")
    Optional<CustomerPrice> findLatestPrice(@Param("customerId") Long customerId,
                                             @Param("productId") Long productId);
}
