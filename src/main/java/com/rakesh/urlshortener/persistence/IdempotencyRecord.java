package com.rakesh.urlshortener.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity @Table(name = "idempotency_keys")
public class IdempotencyRecord {
    @Id @Column(length = 64) private String keyHash;
    @Version private Long version;
    @Column(nullable = false, length = 64) private String fingerprint;
    @Column(nullable = false, length = 32) private String code;
    protected IdempotencyRecord() { }
    public IdempotencyRecord(String keyHash, String fingerprint, String code) {
        this.keyHash = keyHash; this.fingerprint = fingerprint; this.code = code;
    }
    public String getFingerprint() { return fingerprint; }
    public String getCode() { return code; }
}
