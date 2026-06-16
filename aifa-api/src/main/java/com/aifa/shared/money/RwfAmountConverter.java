package com.aifa.shared.money;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RwfAmountConverter implements AttributeConverter<RwfAmount, Long> {

    @Override
    public Long convertToDatabaseColumn(RwfAmount attribute) {
        return attribute == null ? null : attribute.amountRwf();
    }

    @Override
    public RwfAmount convertToEntityAttribute(Long dbData) {
        return dbData == null ? null : RwfAmount.of(dbData);
    }
}
