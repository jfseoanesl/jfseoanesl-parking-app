package com.parking.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cash_concept")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashConceptEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 10)
    private String type;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active;
}
