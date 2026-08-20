package com.geolink.findme.repository;

import com.geolink.findme.entity.OtpCode;
import com.geolink.findme.entity.OtpPurpose;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    @Modifying
    @Query("UPDATE OtpCode o SET o.consumed = true WHERE o.user.id = :userId AND o.purpose = :purpose AND o.consumed = false")
    void consumeAllActiveForUserAndPurpose(@Param("userId") Long userId, @Param("purpose") OtpPurpose purpose);

    Optional<OtpCode> findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(Long userId, OtpPurpose purpose);
}
