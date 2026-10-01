package com.parking.presentation.dto;

import com.parking.domain.model.Configuration;

public record ConfigurationResponse(
        String id, String parkingName, String nit, String address, String phone, Integer maxCapacity,
        String timezone, String currency, String receiptFormatJson
) {
    public static ConfigurationResponse from(Configuration c) {
        return new ConfigurationResponse(c.id(), c.parkingName(), c.nit(), c.address(), c.phone(),
                c.maxCapacity(), c.timezone(), c.currency(), c.receiptFormatJson());
    }
}
