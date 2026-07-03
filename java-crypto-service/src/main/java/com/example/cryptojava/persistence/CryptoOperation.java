package com.example.cryptojava.persistence;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "crypto_operation")
@Getter
@Setter
@NoArgsConstructor
public class CryptoOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String service;

    @Column(nullable = false)
    private String operation;

    @Column(columnDefinition = "bytea")
    private byte[] input;

    @Column(columnDefinition = "bytea")
    private byte[] output;

    @Column(nullable = false)
    private String status;

    @Column
    private String detail;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public CryptoOperation(String service, String operation, byte[] input, byte[] output, String status, String detail) {
        this.service = service;
        this.operation = operation;
        this.input = input;
        this.output = output;
        this.status = status;
        this.detail = detail;
        this.createdAt = OffsetDateTime.now();
    }
}
