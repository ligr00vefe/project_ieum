package com.project.ieum.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "communication_methods",
    uniqueConstraints = @UniqueConstraint(name = "uq_cm_name", columnNames = "name"))
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class CommunicationMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 40)
    private String name;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "sort_order")
    private Short sortOrder;
}
