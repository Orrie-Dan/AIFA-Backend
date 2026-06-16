package com.aifa.shared.money;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public record RwfAmount(long amountRwf) {

    public RwfAmount {
        if (amountRwf < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
    }

    public static RwfAmount of(long amountRwf) {
        return new RwfAmount(amountRwf);
    }

    public static RwfAmount positive(long amountRwf) {
        if (amountRwf <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return new RwfAmount(amountRwf);
    }

    public RwfAmount add(RwfAmount other) {
        return new RwfAmount(this.amountRwf + other.amountRwf);
    }

    public RwfAmount subtract(RwfAmount other) {
        long result = this.amountRwf - other.amountRwf;
        if (result < 0) {
            throw new IllegalArgumentException("Resulting amount cannot be negative");
        }
        return new RwfAmount(result);
    }

    @JsonValue
    public long value() {
        return amountRwf;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RwfAmount other && amountRwf == other.amountRwf;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountRwf);
    }
}
