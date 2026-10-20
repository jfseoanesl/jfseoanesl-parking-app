package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.SubscriptionVehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataSubscriptionVehicleRepository extends JpaRepository<SubscriptionVehicleEntity, String> {

    List<SubscriptionVehicleEntity> findBySubscriptionId(String subscriptionId);

    Optional<SubscriptionVehicleEntity> findByPlate(String plate);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SubscriptionVehicleEntity v where v.subscriptionId = :subscriptionId")
    void deleteBySubscriptionId(@Param("subscriptionId") String subscriptionId);
}
